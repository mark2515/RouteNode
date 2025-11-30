package moe.group13.routenode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import moe.group13.routenode.ui.account.AccountFragment
import moe.group13.routenode.ui.FavoritesFragment
import moe.group13.routenode.ui.search.SearchFragment
import moe.group13.routenode.ui.manual.ManualSearchFragment
import com.google.firebase.firestore.FirebaseFirestore
import moe.group13.routenode.ui.account.ThemeManager
import moe.group13.routenode.ui.map.MapActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ThemeManager.applySavedTheme(this)

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
                R.id.nav_search -> {
                    viewPager.currentItem = 0
                    true
                }

                R.id.nav_favorites -> {
                    viewPager.currentItem = 1
                    true
                }

                R.id.nav_account -> {
                    viewPager.currentItem = 2
                    true
                }

                R.id.map_test -> {
                    val intent = Intent(this, MapActivity::class.java)
                    startActivity(intent)
                    false
                }

                else -> false
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val selectedId = when (position) {
                    0 -> R.id.nav_search
                    1 -> R.id.nav_favorites
                    2 -> R.id.nav_account
                    3 -> R.id.map_test
                    else -> R.id.nav_search
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