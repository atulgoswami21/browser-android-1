package com.cliqz.browser.starttab.freshtab

import acr.browser.lightning.preference.PreferenceManager
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.cliqz.browser.R
import com.cliqz.browser.app.BrowserApp
import com.cliqz.browser.main.Messages
import com.cliqz.browser.purchases.PurchasesManager
import com.cliqz.browser.starttab.StartTabFragment
import com.cliqz.nove.Bus
import kotlinx.android.synthetic.lumen.fragment_freshtab.*
import javax.inject.Inject

private const val SEVEN_DAYS_IN_MILLIS = 604800000L

internal class FreshTab : StartTabFragment() {

    private var trialTries = 1

    @Inject
    lateinit var purchasesManager: PurchasesManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var bus: Bus

    private var isFreshInstall: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        isFreshInstall = arguments?.getBoolean(ARG_IS_FRESH_INSTALL) ?: false
        BrowserApp.getActivityComponent(activity)?.inject(this)
        return inflater.inflate(R.layout.fragment_freshtab, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        toggleWelcomeMessage()
        if (purchasesManager.purchase.isASubscriber) {
            hideAllTrialPeriodViews()
        } else {
            getTrialPeriod()
        }
    }

    private fun getTrialPeriod() {
        // Hacky solution with handler. Bus wasn't working, giving a DispatcherNotFoundException
        // We retry till we get a non null object.
        Handler().postDelayed({
            if (purchasesManager.trialPeriod != null) {
                purchasesManager.trialPeriod?.apply {
                    if (isInTrial) {
                        showTrialDaysLeft(trialDaysLeft)
                    } else {
                        if (preferenceManager.timeWhenTrialMessageDismissed() <
                                (System.currentTimeMillis() - SEVEN_DAYS_IN_MILLIS)) {
                            showTrialPeriodExpired()
                        }
                    }
                }
            } else if (trialTries < 5) {
                trialTries++
                getTrialPeriod()
            }
        }, 200)
    }

    override fun getTitle() = ""

    override fun getIconId() = R.drawable.ic_fresh_tab

    private fun showTrialPeriodExpired() {
        trial_period_lumen_upgrade.visibility = View.GONE
        welcome_image.visibility = View.GONE
        welcome_title.visibility = View.GONE
        trial_over_lumen_upgrade.visibility = View.VISIBLE
        trial_over_learn_more_btn.setOnClickListener {
            trial_over_lumen_upgrade.visibility = View.GONE
            bus.post(Messages.GoToPurchase(0))
        }
        trial_over_dismiss_btn.setOnClickListener {
            trial_over_lumen_upgrade.visibility = View.GONE
            preferenceManager.updateTimeOfTrialMessageDismissed()
        }
    }

    private fun showTrialDaysLeft(daysLeft: Int) {
        trial_period_lumen_upgrade.visibility = View.VISIBLE
        trial_over_lumen_upgrade.visibility = View.GONE
        trial_period_upgrade_description.text =
                resources.getQuantityString(R.plurals.trial_period_upgrade_description, daysLeft, daysLeft)
        trial_period_lumen_upgrade.setOnClickListener {
            bus.post(Messages.GoToPurchase(0))
        }
    }

    private fun hideAllTrialPeriodViews() {
        trial_period_lumen_upgrade.visibility = View.GONE
        trial_over_lumen_upgrade.visibility = View.GONE
    }

    private fun toggleWelcomeMessage() {
        welcome_image.visibility = if (isFreshInstall) View.VISIBLE else View.GONE
        welcome_title.visibility = if (isFreshInstall) View.VISIBLE else View.GONE
    }

    companion object {

        const val ARG_IS_FRESH_INSTALL = "is_fresh_install"

        @JvmStatic
        fun newInstance(isFreshInstall: Boolean) = FreshTab().apply{
            arguments = Bundle().apply {
                putBoolean(ARG_IS_FRESH_INSTALL, isFreshInstall)
            }
        }
    }
}
