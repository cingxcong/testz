// SPDX-License-Identifier: GPL-3.0-only
// Adapted from rushiranpise/morphe-patches Amazon patch sources.
@file:Suppress("unused")

package app.amazon.patches

import app.amazon.extensions.amazonhelper.bindings.AmazonHelper
import app.reseam.patch.*

private const val AMAZON_SHOPPING = "com.amazon.mShop.android.shopping"
private const val AMAZON_INDIA = "in.amazon.mShop.android.shopping"

private val savXTabIsEnabled = klass("com.amazon.mShop.chrome.bottomtabs.SavXTabController")
    .method("isEnabled") { returns(Type.Boolean); params() }
private val mShopWebViewClientOnPageFinished = klass("com.amazon.mShop.web.MShopWebViewClient")
    .method("onPageFinished") {
        returns(Type.Void); params("android.webkit.WebView", Type.String)
        strings("onPageFinished_withDuration")
    }
private val interactionWebFragmentSetWebView = klass("com.amazon.mShop.web.InteractionWebFragment")
    .method("setWebView") { returns(Type.Void); params("com.amazon.mShop.web.MShopWebView") }
private val mShopWebViewInitialize = klass("com.amazon.mShop.web.MShopWebView")
    .method("initialize") { returns(Type.Void); params(); calls { name("setWebViewSettings") } }
private val searchSuggestionsSetEvent = klass(
    "com.amazon.search.resources.dependency.client.suggestions.SearchSuggestionsV2Request\$Builder"
).method("setEvent") {
    returns("com.amazon.search.resources.dependency.client.suggestions.SearchSuggestionsV2Request\$Builder")
    params("com.amazon.search.resources.dependency.client.suggestions.Event")
}
private val mShopWebViewClientShouldOverride = klass("com.amazon.mShop.web.MShopWebViewClient")
    .method("shouldOverrideUrlLoading") {
        returns(Type.Boolean); params("android.webkit.WebView", Type.String); strings("404.html")
    }
private val baseTabControllerGetTabIcon = klass("com.amazon.mShop.chrome.bottomtabs.BaseTabController")
    .method("getTabIcon") { returns("android.widget.ImageView"); params() }

val amazonRemoveAds = patch("Remove ads") {
    description("Hides common sponsored cards and promotional slots in Amazon Shopping WebViews.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute {
        mShopWebViewClientOnPageFinished.before { call(AmazonHelper.injectAdBlock, param(0)) }
        interactionWebFragmentSetWebView.before { call(AmazonHelper.injectAdBlock, param(0)) }
    }
}

val amazonHideRufus = patch("Hide Rufus tab") {
    description("Disables the Rufus AI assistant tab in Amazon's bottom navigation.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute { savXTabIsEnabled.alwaysReturn(false) }
}

val amazonDarkMode = patch("Dark mode") {
    description("Applies dark-mode CSS when Android system dark mode is enabled and tints tab icons.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute {
        mShopWebViewClientOnPageFinished.before { call(AmazonHelper.injectDarkMode, param(0)) }
        baseTabControllerGetTabIcon.after { call(AmazonHelper.tintTabIconIfDark, result) }
    }
}

val amazonDisableVideoAutoplay = patch("Disable video autoplay") {
    description("Requires user interaction before Amazon WebView videos can play.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute { mShopWebViewInitialize.before { call(AmazonHelper.disableAutoplay, thisObject) } }
}

val amazonDisableSearchSuggestionsTracking = patch("Disable search suggestions tracking") {
    description("Drops the event field used to report search suggestion keypress and focus events.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute { searchSuggestionsSetEvent.replace { returnValue(thisObject) } }
}

val amazonOpenLinksInBrowser = patch("Open links in browser") {
    description("Opens non-Amazon HTTP(S) destinations outside the authenticated in-app WebView.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute {
        mShopWebViewClientShouldOverride.before {
            whenTrue(call(AmazonHelper.openExternally, param(0), param(1))) { returnTrue() }
        }
    }
}

val amazonPriceCharts = patch("Price history charts") {
    description("Adds an explicit-load price-history section with Keepa and CamelCamelCamel links on product pages.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute {
        mShopWebViewClientOnPageFinished.before { call(AmazonHelper.injectPriceCharts, param(0), param(1)) }
    }
}

val amazonFixDuplicatePermissions = patch("Fix Amazon manifest conflicts") {
    description("Namespaces declared Amazon permissions so separately installed Amazon apps can coexist.")
    compatibleWith(AMAZON_SHOPPING, AMAZON_INDIA)
    execute {
        manifest.edit {
            val root = findByTag("manifest").firstOrNull() ?: error("manifest root not found")
            val packageName = root["package"] ?: error("manifest package attribute not found")
            val prefix = "$packageName.reseam."
            val declarations = listOf("permission", "permission-group", "permission-tree")
            val references = listOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")
            val renames = linkedMapOf<String, String>()
            for (tag in declarations) for (element in findByTag(tag)) {
                val oldName = element["android:name"] ?: continue
                if (oldName.isBlank() || oldName.startsWith(prefix)) continue
                val newName = prefix + oldName
                element["android:name"] = newName
                renames[oldName] = newName
            }
            for (tag in references) for (element in findByTag(tag)) {
                renames[element["android:name"]]?.let { element["android:name"] = it }
            }
            for (attribute in listOf("android:permission", "android:readPermission", "android:writePermission")) {
                for (element in findByTag("*")) renames[element[attribute]]?.let { element[attribute] = it }
            }
        }
    }
}
