package moe.group13.routenode.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import moe.group13.routenode.R
import android.content.Intent


class AccountFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myProfileBtn = view.findViewById<LinearLayout>(R.id.myProfileBtn)
        val settingsBtn = view.findViewById<LinearLayout>(R.id.SettingsBtn)
        val AIBtn = view.findViewById<LinearLayout>(R.id.AIModelsBtn)
        val logoutBtn = view.findViewById<LinearLayout>(R.id.LogoutBtn)

        myProfileBtn.setOnClickListener { startActivity(Intent(requireContext(), MyProfileActivity::class.java)) }

        settingsBtn.setOnClickListener { startActivity(Intent(requireContext(), SettingsActivity::class.java)) }

    }
}