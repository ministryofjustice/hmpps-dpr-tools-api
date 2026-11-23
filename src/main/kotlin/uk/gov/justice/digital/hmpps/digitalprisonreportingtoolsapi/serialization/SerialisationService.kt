package uk.gov.justice.digital.hmpps.digitalprisonreportingtoolsapi.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

@Service
class SerialisationService(
  private val dprDefinitionKotlinx: Json,
) {

  fun defaultJson(descriptor: SerialDescriptor): JsonElement = when (descriptor.kind) {
    PrimitiveKind.STRING -> JsonPrimitive("")
    PrimitiveKind.INT -> JsonPrimitive(0)
    PrimitiveKind.LONG -> JsonPrimitive(0L)
    PrimitiveKind.FLOAT -> JsonPrimitive(0f)
    PrimitiveKind.DOUBLE -> JsonPrimitive(0.0)
    PrimitiveKind.BOOLEAN -> JsonPrimitive(false)
    PrimitiveKind.SHORT -> JsonPrimitive(0)
    PrimitiveKind.BYTE -> JsonPrimitive(0)
    PrimitiveKind.CHAR -> JsonPrimitive("")

    StructureKind.LIST -> JsonArray(emptyList())

    StructureKind.MAP -> JsonObject(emptyMap())

    is SerialKind.ENUM ->
      JsonPrimitive(descriptor.getElementName(0))

    StructureKind.CLASS ->
      JsonObject(
        buildMap {
          for (i in 0 until descriptor.elementsCount) {
            val childDescriptor =
              descriptor.getElementDescriptor(i)

            put(
              descriptor.getElementName(i),
              defaultJson(childDescriptor),
            )
          }
        },
      )

    else -> JsonNull
  }

  fun setAtPath(
    element: JsonElement,
    path: List<Any>,
    value: JsonElement,
  ): JsonElement {
    if (path.isEmpty()) return value

    return when (element) {
      is JsonObject -> {
        val key = path.first() as String
        JsonObject(
          element + (
            key to setAtPath(
              element[key] ?: JsonNull,
              path.drop(1),
              value,
            )
            ),
        )
      }

      is JsonArray -> {
        val index = path.first() as Int

        JsonArray(
          element.mapIndexed { i, e ->
            if (i == index) {
              setAtPath(e, path.drop(1), value)
            } else {
              e
            }
          },
        )
      }

      else -> error("Cannot descend into $element")
    }
  }

  fun parsePath(path: String): List<Any> {
    val result = mutableListOf<Any>()

    var current = path.removePrefix("$.")

    while (current.isNotEmpty()) {
      when {
        current.startsWith('[') -> {
          val end = current.indexOf(']')
          result += current.substring(1, end).toInt()
          current = current.substring(end + 1)
        }

        current.startsWith('.') -> {
          current = current.substring(1)
        }

        else -> {
          val dot = current.indexOf('.')
          val bracket = current.indexOf('[')

          val end = listOf(dot, bracket)
            .filter { it >= 0 }
            .minOrNull()
            ?: current.length

          result += current.substring(0, end)
          current = current.substring(end)
        }
      }
    }

    return result
  }

  fun descriptorAtPath(
    descriptor: SerialDescriptor,
    path: List<Any>,
  ): SerialDescriptor = when {
    path.isEmpty() -> descriptor

    path.first() is String -> {
      val field = path.first() as String

      val index = descriptor.getElementIndex(field)

      require(index != CompositeDecoder.UNKNOWN_NAME) {
        "Field '$field' not found in ${descriptor.serialName}"
      }

      descriptorAtPath(
        descriptor.getElementDescriptor(index),
        path.drop(1),
      )
    }

    path.first() is Int -> {
      require(descriptor.kind == StructureKind.LIST) {
        "Expected LIST but got ${descriptor.kind}"
      }

      descriptorAtPath(
        descriptor.getElementDescriptor(0),
        path.drop(1),
      )
    }

    else -> error("Unexpected path component: ${path.first()}")
  }

  enum class DPDIssueType(val description: String) {
    MISSING_FIELD("A required field is missing"),
    INVALID_FIELD_VALUE("This field has an invalid value"),
    TRAILING_COMMA("There is a trailing comma"),
    GENERAL_SYNTAX_ERROR("There is a general syntax error that cannot be otherwise detected"),
    UNKNOWN_ISSUE("There is an unknown issue"),
  }

  data class DPDIssue(
    val path: String,
    val issueType: DPDIssueType,
    val extraHint: String? = null,
  ) {
    val issueTypeDescription: String = issueType.description
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun deserializeProductDefinition(input: String): Pair<ProductDefinition?, List<DPDIssue>> {
    val issues = mutableListOf<DPDIssue>()
    try {
      return Pair(dprDefinitionKotlinx.decodeFromString<ProductDefinition>(input), issues)
    } catch (jde: JsonDecodingException) {
      if (jde.message.lowercase().contains("trailing comma")) {
        issues.add(DPDIssue(jde.path!!, DPDIssueType.TRAILING_COMMA))
        return Pair(null, issues)
      }
      if (jde.message.lowercase().contains("unexpected json token")) {
        if (jde.message.lowercase().contains("expected valid boolean literal")) {
          val el = dprDefinitionKotlinx.parseToJsonElement(input)
          val path = parsePath(jde.path!!)

          val descriptor = descriptorAtPath(
            ProductDefinition.serializer().descriptor,
            path,
          )

          val updated = setAtPath(
            el,
            path,
            defaultJson(descriptor),
          )
          val extraHint = jde.input
          issues.add(DPDIssue(jde.path!!, DPDIssueType.INVALID_FIELD_VALUE, extraHint))
          val (_, newIssues) = deserializeProductDefinition(dprDefinitionKotlinx.encodeToString(updated))
          issues.addAll(newIssues)
          return Pair(null, issues)
        }
        issues.add(DPDIssue(jde.path!!, DPDIssueType.GENERAL_SYNTAX_ERROR, jde.input))
        return Pair(null, issues)
      }
      issues.add(DPDIssue(jde.path!!, DPDIssueType.UNKNOWN_ISSUE, jde.message))
      return Pair(null, issues)
    } catch (mfe: MissingFieldException) {
      val el = dprDefinitionKotlinx.parseToJsonElement(input)

      val missingField = mfe.missingFields.first()

      val pathString = "\$${mfe.message?.split("$")?.get(1)}"
      val parentPath = parsePath(if (pathString == "$") "" else pathString)

      val parentDescriptor =
        descriptorAtPath(
          ProductDefinition.serializer().descriptor,
          parentPath,
        )

      val idx = parentDescriptor.getElementIndex(missingField)

      val missingDescriptor =
        parentDescriptor.getElementDescriptor(idx)

      val updated = setAtPath(
        el,
        parentPath + missingField,
        defaultJson(missingDescriptor),
      )
      issues.add(DPDIssue("$pathString.$missingField", DPDIssueType.MISSING_FIELD))
      val (_, newIssues) = deserializeProductDefinition(dprDefinitionKotlinx.encodeToString(updated))
      issues.addAll(newIssues)
    } catch (se: SerializationException) {
      val el = dprDefinitionKotlinx.parseToJsonElement(input)

      val pathString = "\$${se.message?.split("$")[1]}"
      val path = parsePath(pathString)

      val descriptor = descriptorAtPath(
        ProductDefinition.serializer().descriptor,
        path,
      )

      val updated = setAtPath(
        el,
        path,
        defaultJson(descriptor),
      )
      var extraHint: String? = null
      if (descriptor.kind == SerialKind.ENUM) {
        extraHint = "Potential values are: " + (0 until descriptor.elementsCount)
          .map(descriptor::getElementName)
      }
      issues.add(DPDIssue(pathString, DPDIssueType.INVALID_FIELD_VALUE, extraHint))
      val (_, newIssues) = deserializeProductDefinition(dprDefinitionKotlinx.encodeToString(updated))
      issues.addAll(newIssues)
    } catch (other: Throwable) {
      throw other
    }
    return Pair(null, issues)
  }
}
