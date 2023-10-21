package parsers

import com.google.gson.annotations.SerializedName
import models.DataObject
import models.Point

data class Place(
    @SerializedName("id") private var _id: String? = null,
    @SerializedName("geometry") private var geometry: Geometry? = Geometry(),
    @SerializedName("properties") private var properties: Properties? = Properties()
) {
    val coordinate: Point
        get() {
            return Point(geometry!!.coordinates[0], geometry!!.coordinates[1])
        }
    val keywords: List<String>
        get() {
            return properties?.tags ?: emptyList()
        }

    fun toDataObject(oid: Int, expireTimestamp: Int): DataObject {
        return DataObject(oid, keywords, coordinate, expireTimestamp)
    }

    override fun toString(): String {
        return "Place(id=$_id, coordinates=$coordinate, keywords=$keywords)"
    }
}

data class Geometry(
    @SerializedName("type") var type: String? = null,
    @SerializedName("coordinates") var coordinates: ArrayList<Double> = arrayListOf()
) {
    override fun toString(): String {
        return "Geometry(type=$type, coordinates=$coordinates)"
    }
}

data class Properties(
    @SerializedName("tags") var tags: ArrayList<String>? = arrayListOf(),
) {
    override fun toString(): String {
        return "Properties(tags=$tags)"
    }
}