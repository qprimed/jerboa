package com.jerboa.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.apiWrapper
import com.jerboa.datatypes.types.GetSite
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.GetUnreadCount
import com.jerboa.datatypes.types.GetUnreadCountResponse
import com.jerboa.datatypes.types.ListingType
import com.jerboa.datatypes.types.SortType
import com.jerboa.db.entity.Account
import com.jerboa.serializeToMap
import kotlinx.coroutines.launch

class SiteViewModel : ViewModel() {

    // Can't be private, because it needs to be set by the login viewmodel
    var siteRes: ApiState<GetSiteResponse> by mutableStateOf(ApiState.Empty)

    private var unreadCountRes: ApiState<GetUnreadCountResponse> by mutableStateOf(ApiState.Empty)

    var sortType by mutableStateOf(SortType.Active)
        private set
    var listingType by mutableStateOf(ListingType.Local)
        private set

    fun updateSortType(sortType: SortType) {
        this.sortType = sortType
    }

    fun updateListingType(listingType: ListingType) {
        this.listingType = listingType
    }

    fun updateFromAccount(account: Account) {
        updateSortType(SortType.values().getOrElse(account.defaultSortType) { sortType })
        updateListingType(ListingType.values().getOrElse(account.defaultListingType) { listingType })
    }

    fun getSite(
        form: GetSite,
    ) {
        viewModelScope.launch {
            siteRes = ApiState.Loading
            siteRes = apiWrapper(API.getInstance().getSite(form.serializeToMap()))

            when (val res = siteRes) {
                is ApiState.Success -> {
                    res.data.my_user?.local_user_view?.local_user?.let {
                        updateSortType(it.default_sort_type)
                        updateListingType(it.default_listing_type)
                    }
                }
                else -> {}
            }
        }
    }

    fun fetchUnreadCounts(
        form: GetUnreadCount,
    ) {
        viewModelScope.launch {
            viewModelScope.launch {
                unreadCountRes = ApiState.Loading
                unreadCountRes = apiWrapper(API.getInstance().getUnreadCount(form.serializeToMap()))
            }
        }
    }

    fun getUnreadCountTotal(): Int {
        return when (val res = unreadCountRes) {
            is ApiState.Success -> {
                val unreads = res.data
                unreads.mentions + unreads.private_messages + unreads.replies
            }
            else -> 0
        }
    }

    fun showAvatar(): Boolean {
        return when (val res = siteRes) {
            is ApiState.Success -> res.data.my_user?.local_user_view?.local_user?.show_avatars ?: true
            else -> true
        }
    }

    fun enableDownvotes(): Boolean {
        return when (val res = siteRes) {
            is ApiState.Success -> res.data.site_view.local_site.enable_downvotes
            else -> true
        }
    }
}