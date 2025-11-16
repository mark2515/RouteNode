package moe.group13.routenode.ui.manual

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index


@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val routeId: Long = 0,
    val name: String
)
//idea: https://stackoverflow.com/questions/47511750/how-to-use-foreign-key-in-room-persistence-library
@Entity(
    tableName = "restaurants",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["routeId"],
        childColumns = ["routeOwnerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeOwnerId")]
)
data class RestaurantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val routeOwnerId: Long
)
