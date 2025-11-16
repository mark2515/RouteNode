package moe.group13.routenode.ui.manual

import androidx.room.*

@Dao
interface RouteDao {
    @Insert
    suspend fun insertRoute(route: RouteEntity): Long

    @Query("SELECT * FROM routes")
    suspend fun getAllRoutes(): List<RouteEntity>
}

@Dao
interface RestaurantDao {
    @Insert
    suspend fun insertRestaurant(restaurant: RestaurantEntity)

    @Query("SELECT * FROM restaurants WHERE routeOwnerId = :routeId")
    suspend fun getRestaurantsForRoute(routeId: Long): List<RestaurantEntity>
}
