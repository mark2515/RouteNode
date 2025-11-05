package moe.group13.routenode

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import moe.group13.routenode.ui.AccountFragment
import moe.group13.routenode.ui.FavoritesFragment
import moe.group13.routenode.ui.SearchFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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