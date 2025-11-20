package moe.group13.routenode

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import moe.group13.routenode.ui.account.AccountFragment
import moe.group13.routenode.ui.FavoritesFragment
import moe.group13.routenode.ui.search.SearchFragment
import moe.group13.routenode.ui.manual.ManualSearchFragment
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // DB check needed

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> SearchFragment()
                    1 -> FavoritesFragment()
                    2 -> AccountFragment()
                    3 -> ManualSearchFragment()
                    else -> SearchFragment()
                }
            }
        }
        viewPager.offscreenPageLimit = 4

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> viewPager.currentItem = 0
                R.id.nav_favorites -> viewPager.currentItem = 1
                R.id.nav_account -> viewPager.currentItem = 2
                R.id.nav_manual_search -> viewPager.currentItem = 3
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val selectedId = when (position) {
                    0 -> R.id.nav_search
                    1 -> R.id.nav_favorites
                    2 -> R.id.nav_account
                    3 -> R.id.nav_manual_search
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