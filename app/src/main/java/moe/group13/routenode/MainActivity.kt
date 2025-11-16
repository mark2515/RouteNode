package moe.group13.routenode

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import moe.group13.routenode.ui.account.AccountFragment
import moe.group13.routenode.ui.FavoritesFragment
import moe.group13.routenode.ui.SearchFragment
import com.google.firebase.firestore.FirebaseFirestore
import moe.group13.routenode.data.repository.RouteRepository
import moe.group13.routenode.data.model.Route

class MainActivity : AppCompatActivity() {
//    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TEST DATABASE
//        val testData = hashMapOf("message" to "Hello Firebase!")
//        db.collection("test").add(testData)
//            .addOnSuccessListener { documentReference ->
//                Log.d("FIREBASE", "DocumentSnapshot added with ID: ${documentReference.id}")
//            }
//            .addOnFailureListener { e ->
//                Log.w("FIREBASE", "Error adding document", e)
//            }

        // Testing DB
        val repo = RouteRepository()
        val sampleRoute = Route(
            title = "Test Route",
            description = "This is a testing route",
            distanceKm = 10.0,
            creatorId = "user123",
            isPublic = true
        )

        repo.saveRoute( sampleRoute){ success, id ->
            if (success) {
                Log.d("FIREBASE", "Route saved with ID: $id")
            } else {
                Log.e("FIREBASE", "Failed to save route")
            }

        }


        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        val fragments = listOf(
            SearchFragment(),
            FavoritesFragment(),
            AccountFragment()
        )

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }
        viewPager.offscreenPageLimit = fragments.size

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> viewPager.currentItem = 0
                R.id.nav_favorites -> viewPager.currentItem = 1
                R.id.nav_account -> viewPager.currentItem = 2
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val selectedId = when (position) {
                    0 -> R.id.nav_search
                    1 -> R.id.nav_favorites
                    else -> R.id.nav_account
                }
                if (bottomNav.selectedItemId != selectedId) {
                    bottomNav.selectedItemId = selectedId
                }
            }
        })

        bottomNav.selectedItemId = R.id.nav_search
        viewPager.setCurrentItem(0, false)
    }
}