package moe.group13.routenode.ui.routes

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import moe.group13.routenode.R
import moe.group13.routenode.data.model.Route

class RouteDetailsActivity : AppCompatActivity() {
    
    private lateinit var viewModel: RouteViewModel
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvCreator: TextView
    private lateinit var tvDifficulty: TextView
    private lateinit var tvDuration: TextView
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnShare: Button
    
    private var currentRoute: Route? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_details)
        
        viewModel = ViewModelProvider(this)[RouteViewModel::class.java]
        
        initViews()
        loadRouteData()
        setupListeners()
        observeViewModel()
    }
    
    private fun initViews() {
        tvTitle = findViewById(R.id.tvRouteTitle)
        tvDescription = findViewById(R.id.tvRouteDescription)
        tvDistance = findViewById(R.id.tvRouteDistance)
        tvCreator = findViewById(R.id.tvRouteCreator)
        tvDifficulty = findViewById(R.id.tvRouteDifficulty)
        tvDuration = findViewById(R.id.tvRouteDuration)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnShare = findViewById(R.id.btnShare)
        
        // Set up back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
    
    private fun loadRouteData() {
        val routeId = intent.getStringExtra("route_id")
        val routeTitle = intent.getStringExtra("route_title")
        val routeDescription = intent.getStringExtra("route_description")
        val routeDistance = intent.getDoubleExtra("route_distance", 0.0)
        
        if (routeId != null) {
            // Load full route data from ViewModel
            viewModel.loadPublicRoutes()
            viewModel.loadFavorites()
        } else {
            // Use passed data for display (fallback if route not found in ViewModel)
            tvTitle.text = routeTitle ?: "Route"
            tvDescription.text = routeDescription ?: "No description"
            tvDistance.text = "${routeDistance} km"
            
            // Create a temporary route object for favorite functionality
            currentRoute = Route(
                id = "",
                title = routeTitle ?: "Route",
                description = routeDescription ?: "",
                distanceKm = routeDistance
            )
        }
    }
    
    private fun setupListeners() {
        btnFavorite.setOnClickListener {
            currentRoute?.let { route ->
                viewModel.toggleFavorite(route)
            }
        }
        
        btnShare.setOnClickListener {
            shareRoute()
        }
    }
    
    private fun observeViewModel() {
        viewModel.publicRoutes.observe(this) { routes ->
            val routeId = intent.getStringExtra("route_id")
            val route = routes.find { it.id == routeId }
            if (route != null) {
                currentRoute = route
                displayRoute(route)
            }
        }
        
        viewModel.favorites.observe(this) { favorites ->
            currentRoute?.let { route ->
                val isFavorited = favorites.any { it.id == route.id }
                updateFavoriteButton(isFavorited)
            }
        }
        
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun displayRoute(route: Route) {
        tvTitle.text = route.title
        tvDescription.text = route.description
        tvDistance.text = "${route.distanceKm} km"
        tvCreator.text = route.creatorName.takeIf { it.isNotBlank() } ?: "Unknown"
        tvDifficulty.text = route.difficulty.replaceFirstChar { it.uppercaseChar() }
        tvDuration.text = "${route.estimatedDurationMinutes} min"
        
        // Check if favorited
        val isFavorited = viewModel.favorites.value?.any { it.id == route.id } ?: false
        updateFavoriteButton(isFavorited)
    }
    
    private fun updateFavoriteButton(isFavorited: Boolean) {
        btnFavorite.setImageResource(
            if (isFavorited) android.R.drawable.btn_star_big_on
            else android.R.drawable.btn_star_big_off
        )
        btnFavorite.contentDescription = if (isFavorited) "Remove from favorites" else "Add to favorites"
    }
    
    private fun shareRoute() {
        currentRoute?.let { route ->
            val shareText = "${route.title}\n${route.description}\nDistance: ${route.distanceKm} km"
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(sendIntent, "Share route via"))
        } ?: run {
            Toast.makeText(this, "Route information not available", Toast.LENGTH_SHORT).show()
        }
    }
}

