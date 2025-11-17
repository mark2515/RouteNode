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
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PlacesAutoCompleteAdapter(
    context: Context,
    private val placesClient: PlacesClient
) : ArrayAdapter<PlacesAutoCompleteAdapter.PlaceAutocomplete>(
    context,
    android.R.layout.simple_dropdown_item_1line
), Filterable {

    private var resultList: ArrayList<PlaceAutocomplete> = ArrayList()
    private val token: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

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
                
                if (constraint != null) {
                    resultList = getPredictions(constraint)
                    filterResults.values = resultList
                    filterResults.count = resultList.size
                }
                
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
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

    private fun getPredictions(constraint: CharSequence): ArrayList<PlaceAutocomplete> {
        val resultList = ArrayList<PlaceAutocomplete>()

        // Create a new request with the typed text
        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(constraint.toString())
            .build()

        try {
            val response = Tasks.await(placesClient.findAutocompletePredictions(request), 3, TimeUnit.SECONDS)
            
            for (prediction in response.autocompletePredictions) {
                Log.d("PlacesAutoComplete", "Prediction: ${prediction.getFullText(null)}")
                
                resultList.add(
                    PlaceAutocomplete(
                        placeId = prediction.placeId,
                        primaryText = prediction.getPrimaryText(null).toString(),
                        secondaryText = prediction.getSecondaryText(null).toString()
                    )
                )
            }
        } catch (e: ExecutionException) {
            Log.e("PlacesAutoComplete", "Error getting predictions: ${e.message}")
        } catch (e: InterruptedException) {
            Log.e("PlacesAutoComplete", "Error getting predictions: ${e.message}")
        } catch (e: TimeoutException) {
            Log.e("PlacesAutoComplete", "Timeout getting predictions")
        } catch (e: ApiException) {
            Log.e("PlacesAutoComplete", "API Error getting predictions: ${e.message}")
        }

        return resultList
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
}