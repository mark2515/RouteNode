package moe.group13.routenode.ui.search

import android.content.Context
import android.graphics.Typeface
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.*

class PlacesAutoCompleteAdapter(
    context: Context,
    private val placesClient: PlacesClient
) : ArrayAdapter<PlacesAutoCompleteAdapter.PlaceAutocomplete>(
    context,
    android.R.layout.simple_dropdown_item_1line
), Filterable {

    private var resultList: ArrayList<PlaceAutocomplete> = ArrayList()
    private val token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()
    
    // Use a coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    data class PlaceAutocomplete(
        val placeId: String,
        val primaryText: String,
        val secondaryText: String
    ) {
        override fun toString(): String {
            return "$primaryText, $secondaryText"
        }
    }

    override fun getCount(): Int = resultList.size

    override fun getItem(position: Int): PlaceAutocomplete? = resultList[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val item = getItem(position)
        
        if (view is TextView && item != null) {
            view.text = item.toString()
        }
        
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                
                if (constraint != null && constraint.isNotEmpty()) {
                    // Trigger async prediction fetch
                    getPredictionsAsync(constraint.toString())
                } else {
                    resultList.clear()
                    filterResults.values = resultList
                    filterResults.count = 0
                }
                
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is PlaceAutocomplete) {
                    resultValue.toString()
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }


    private fun getPredictionsAsync(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val newResultList = ArrayList<PlaceAutocomplete>()
                
                for (prediction in response.autocompletePredictions) {
                    Log.d("PlacesAutoComplete", "Prediction: ${prediction.getFullText(null)}")
                    
                    newResultList.add(
                        PlaceAutocomplete(
                            placeId = prediction.placeId,
                            primaryText = prediction.getPrimaryText(null).toString(),
                            secondaryText = prediction.getSecondaryText(null).toString()
                        )
                    )
                }
                
                // Update results on main thread
                resultList = newResultList
                notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                when (exception) {
                    is ApiException -> {
                        Log.e("PlacesAutoComplete", "API Error getting predictions: ${exception.message}")
                    }
                    else -> {
                        Log.e("PlacesAutoComplete", "Error getting predictions: ${exception.message}")
                    }
                }
                
                // Clear results on error
                resultList.clear()
                notifyDataSetInvalidated()
            }
    }

    fun getPlaceDetails(placeId: String, callback: (Place?) -> Unit) {
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            callback(response.place)
        }.addOnFailureListener { exception ->
            Log.e("PlacesAutoComplete", "Error fetching place details: ${exception.message}")
            callback(null)
        }
    }
    
    fun cleanup() {
        scope.cancel()
    }
}