package com.family.tree.core.genealogy.parser

import com.family.tree.core.genealogy.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Интерфейс для парсеров различных генеалогических форматов.
 */
interface GenealogyDataParser {
    /**
     * Парсинг данных в унифицированную модель ExternalPerson.
     */
    fun parse(data: String): Result<List<ExternalPerson>>
    
    /**
     * Парсинг одной персоны.
     */
    fun parseSingle(data: String): Result<ExternalPerson>
}

/**
 * Парсер для GEDCOM X формата (JSON).
 * Спецификация: http://www.gedcomx.org/
 */
class GedcomXParser : GenealogyDataParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    override fun parse(data: String): Result<List<ExternalPerson>> {
        return try {
            val jsonElement = json.parseToJsonElement(data).jsonObject
            val persons = jsonElement["persons"]?.jsonArray ?: return Result.success(emptyList())
            
            val parsedPersons = persons.map { personElement ->
                parsePersonFromGedcomX(personElement.jsonObject)
            }
            
            Result.success(parsedPersons)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun parseSingle(data: String): Result<ExternalPerson> {
        return try {
            val jsonElement = json.parseToJsonElement(data).jsonObject
            val persons = jsonElement["persons"]?.jsonArray
            
            if (persons.isNullOrEmpty()) {
                return Result.failure(IllegalArgumentException("No persons found in data"))
            }
            
            val person = parsePersonFromGedcomX(persons[0].jsonObject)
            Result.success(person)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Парсинг персоны из GEDCOM X JSON объекта.
     */
    private fun parsePersonFromGedcomX(personJson: kotlinx.serialization.json.JsonObject): ExternalPerson {
        val id = personJson["id"]?.jsonPrimitive?.content ?: ""
        
        // Парсинг имён
        val names = parseNames(personJson["names"]?.jsonArray)
        
        // Парсинг пола
        val gender = personJson["gender"]?.jsonObject?.get("type")?.jsonPrimitive?.content
        
        // Парсинг фактов (события)
        val facts = personJson["facts"]?.jsonArray
        val events = mutableListOf<ExternalEvent>()
        var birthDate: ExternalDate? = null
        var birthPlace: ExternalPlace? = null
        var deathDate: ExternalDate? = null
        var deathPlace: ExternalPlace? = null
        
        facts?.forEach { factElement ->
            val fact = factElement.jsonObject
            val type = fact["type"]?.jsonPrimitive?.content ?: ""
            val date = fact["date"]?.jsonObject?.let { parseDate(it) }
            val place = fact["place"]?.jsonObject?.let { parsePlace(it) }
            
            when (type) {
                "http://gedcomx.org/Birth" -> {
                    birthDate = date
                    birthPlace = place
                }
                "http://gedcomx.org/Death" -> {
                    deathDate = date
                    deathPlace = place
                }
                else -> {
                    events.add(ExternalEvent(
                        type = type.substringAfterLast("/"),
                        date = date,
                        place = place
                    ))
                }
            }
        }
        
        return ExternalPerson(
            externalId = id,
            sourceProvider = "GEDCOM_X",
            firstName = names.first,
            middleName = names.middle,
            lastName = names.last,
            gender = gender?.substringAfterLast("/"),
            birthDate = birthDate,
            birthPlace = birthPlace,
            deathDate = deathDate,
            deathPlace = deathPlace,
            events = events,
            rawData = personJson.toString()
        )
    }
    
    /**
     * Парсинг имён из GEDCOM X.
     */
    private fun parseNames(namesArray: kotlinx.serialization.json.JsonArray?): Names {
        if (namesArray.isNullOrEmpty()) {
            return Names("", "", "")
        }
        
        val nameObj = namesArray[0].jsonObject
        val nameForms = nameObj["nameForms"]?.jsonArray
        
        if (nameForms.isNullOrEmpty()) {
            return Names("", "", "")
        }
        
        val nameForm = nameForms[0].jsonObject
        val parts = nameForm["parts"]?.jsonArray
        
        var firstName = ""
        var middleName = ""
        var lastName = ""
        
        parts?.forEach { partElement ->
            val part = partElement.jsonObject
            val type = part["type"]?.jsonPrimitive?.content ?: ""
            val value = part["value"]?.jsonPrimitive?.content ?: ""
            
            when (type) {
                "http://gedcomx.org/Given" -> firstName = value
                "http://gedcomx.org/Surname" -> lastName = value
            }
        }
        
        return Names(firstName, middleName, lastName)
    }
    
    /**
     * Парсинг даты из GEDCOM X.
     */
    private fun parseDate(dateObj: kotlinx.serialization.json.JsonObject): ExternalDate {
        val original = dateObj["original"]?.jsonPrimitive?.content ?: ""
        val formal = dateObj["formal"]?.jsonPrimitive?.content
        
        // Упрощённый парсинг даты
        val year = formal?.take(4)?.toIntOrNull()
        
        return ExternalDate(
            original = original,
            normalized = formal,
            year = year
        )
    }
    
    /**
     * Парсинг места из GEDCOM X.
     */
    private fun parsePlace(placeObj: kotlinx.serialization.json.JsonObject): ExternalPlace {
        val original = placeObj["original"]?.jsonPrimitive?.content ?: ""
        
        return ExternalPlace(
            original = original,
            normalized = original
        )
    }
    
    private data class Names(
        val first: String,
        val middle: String,
        val last: String
    )
}

/**
 * Парсер для GEDCOM 5.5 формата (текстовый).
 */
class Gedcom55Parser : GenealogyDataParser {
    
    override fun parse(data: String): Result<List<ExternalPerson>> {
        return try {
            // TODO: Реализовать парсинг GEDCOM 5.5
            // Можно использовать существующий GedcomImporter
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun parseSingle(data: String): Result<ExternalPerson> {
        return try {
            // TODO: Реализовать парсинг GEDCOM 5.5
            Result.failure(NotImplementedError("GEDCOM 5.5 parser not implemented yet"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Фабрика для создания парсеров.
 */
object GenealogyParserFactory {
    
    fun createParser(format: String): GenealogyDataParser {
        return when (format.uppercase()) {
            "GEDCOM_X" -> GedcomXParser()
            "GEDCOM_5_5" -> Gedcom55Parser()
            else -> GedcomXParser() // По умолчанию GEDCOM X
        }
    }
}
