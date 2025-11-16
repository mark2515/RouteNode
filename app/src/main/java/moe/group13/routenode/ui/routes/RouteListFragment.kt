package moe.group13.routenode.ui.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.group13.routenode.R

class RouteListFragment : Fragment() {
    private val viewModel: RouteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_route_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.routeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val adapter = RouteAdapter(emptyList()){ route ->
            // Handle route click
        }
        recyclerView.adapter = adapter

        viewModel.publicRoutes.observe(viewLifecycleOwner) {
            adapter.update(it)
        }

        viewModel.loadPublicRoutes()
    }
}
