@file:JvmName("AmazonHelper")
@file:Suppress("unused")

package app.amazon.extensions.amazonhelper

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.webkit.WebView
import android.widget.ImageView
import app.reseam.runtime.Export
import java.net.URI
import java.util.Locale

private val adSelectors = listOf(
    ".s-result-item.AdHolder", ".s-result-item:has(.puis-sponsored-label-text)",
    ".p13n-sc-unified-ad-label", ".sbv-video-single-product", "#sponsoredProducts_feature_div",
    "#sponsoredProducts2_feature_div", "#detailILM_feature_div", "#nav-swmslot",
    "#mobile-mshop-ad", "[data-ad-id]", "[class*=SponsoredProducts]", "[data-cel-widget^=mobile-ads-]",
    "#sims-fbt", "#sims-fbt-content"
)

@Export
fun injectAdBlock(webView: WebView?) {
    if (webView == null) return
    val css = adSelectors.joinToString("") { "$it{display:none!important}" }
    webView.evaluateJavascript("(function(c){var s=document.getElementById('reseam-amazon-ads');" +
        "if(!s){s=document.createElement('style');s.id='reseam-amazon-ads';" +
        "(document.head||document.documentElement).appendChild(s);}s.textContent=c;})(" + jsString(css) + ");", null)
}

private const val DARK_CSS = "html,body{background:#1a1a1a!important;color:#eee!important}" +
    "[class*=image-container] img{mix-blend-mode:normal!important}" +
    "img[style*=mix-blend-mode]{mix-blend-mode:normal!important}" +
    "[class*=asin-container],[class*=asin-image-wrapper]{background:#fff!important}" +
    ".a-button-primary,.a-button-oneclick{color-scheme:only light!important}"

private fun systemDark(view: android.view.View): Boolean =
    (view.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

@Export
fun injectDarkMode(webView: WebView?) {
    if (webView == null || !systemDark(webView)) return
    webView.evaluateJavascript("(function(c){var s=document.getElementById('reseam-amazon-dark');" +
        "if(!s){s=document.createElement('style');s.id='reseam-amazon-dark';" +
        "(document.head||document.documentElement).appendChild(s);}s.textContent=c;})(" + jsString(DARK_CSS) + ");", null)
}

@Export
fun tintTabIconIfDark(icon: ImageView?) {
    if (icon != null && systemDark(icon)) icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
}

@Export
fun disableAutoplay(webView: WebView?) { webView?.settings?.mediaPlaybackRequiresUserGesture = true }

private fun amazonHost(url: String?): String? = try {
    if (url.isNullOrBlank()) null else {
        val uri = URI(url.trim())
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if ((scheme != "http" && scheme != "https") || uri.rawUserInfo != null) null
        else uri.host?.lowercase(Locale.ROOT)?.trimEnd('.')?.takeIf { it.isNotBlank() }
    }
} catch (_: Exception) { null }

private fun isAmazonHost(host: String): Boolean = listOf(
    "amazon.com", "amazon.co.uk", "amazon.de", "amazon.fr", "amazon.co.jp", "amazon.ca",
    "amazon.it", "amazon.es", "amazon.in", "amazon.nl", "amazon.se", "amazon.pl",
    "amazon.ie", "amazon.ae", "amazon.sa", "amazon.eg", "amazon.sg", "amazon.com.au",
    "amazon.com.br", "amazon.com.mx", "amazon.com.be", "amazon.com.tr",
    "media-amazon.com", "ssl-images-amazon.com"
).any { host == it || host.endsWith(".$it") }

@Export
fun openExternally(webView: WebView?, url: String?): Boolean {
    val host = amazonHost(url) ?: return true
    if (isAmazonHost(host)) return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url!!.trim()))
            .addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        webView?.context?.startActivity(intent)
        true
    } catch (_: Exception) { true }
}

@Export
fun injectPriceCharts(webView: WebView?, url: String?) {
    if (webView == null || url.isNullOrBlank()) return
    val host = amazonHost(url) ?: return
    if (!isAmazonHost(host)) return
    val path = try { URI(url).path ?: "" } catch (_: Exception) { "" }
    val asin = Regex("/(?:dp|gp/product|gp/aw/d)/([A-Za-z0-9]{10})(?![A-Za-z0-9])")
        .find(path)?.groupValues?.get(1)?.uppercase(Locale.ROOT) ?: return
    val domain = when {
        host.endsWith("amazon.co.uk") -> "uk"
        host.endsWith("amazon.de") -> "de"
        host.endsWith("amazon.fr") -> "fr"
        host.endsWith("amazon.co.jp") -> "jp"
        host.endsWith("amazon.ca") -> "ca"
        host.endsWith("amazon.it") -> "it"
        host.endsWith("amazon.es") -> "es"
        host.endsWith("amazon.in") -> "in"
        host.endsWith("amazon.com.au") -> "au"
        else -> "us"
    }
    val js = """
        (function(){
          if(document.getElementById('reseam-price-history'))return;
          var asin=ASIN_TOKEN;
          var box=document.createElement('details');box.id='reseam-price-history';
          box.style.cssText='margin:14px 0;padding:12px;border:1px solid #ddd;border-radius:8px';
          var title=document.createElement('summary');title.textContent='Price history';title.style.fontWeight='bold';box.appendChild(title);
          var note=document.createElement('p');note.textContent='Charts load only after you tap the button. The product ASIN is shared with the chart providers.';box.appendChild(note);
          var b=document.createElement('button');b.textContent='Load price history';b.type='button';box.appendChild(b);
          b.onclick=function(){
            b.remove();
            var ids={'us':1,'uk':2,'de':3,'fr':4,'jp':5,'ca':6,'it':8,'es':9,'in':10,'mx':11,'br':12,'nl':14};
            var k=document.createElement('a');k.href='https://keepa.com/#!product/'+(ids[DOMAIN_TOKEN]||1)+'-'+asin;
            k.textContent='Open Keepa price history';k.target='_blank';k.rel='noopener noreferrer';box.appendChild(k);
            var c=document.createElement('a');c.href='https://www.camelcamelcamel.com/product/'+asin;
            c.textContent='Open CamelCamelCamel price history';c.target='_blank';c.rel='noopener noreferrer';c.style.display='block';box.appendChild(c);
          };
          var target=document.querySelector('#buyBoxAccordion,#corePriceDisplay_desktop_feature_div,#corePrice_feature_div,#unifiedPrice_feature_div,#buybox,#centerCol');
          if(target&&target.parentNode)target.parentNode.insertBefore(box,target.nextSibling);
        })();
    """.trimIndent().replace("ASIN_TOKEN", jsString(asin)).replace("DOMAIN_TOKEN", jsString(domain))
    webView.evaluateJavascript(js, null)
}

private fun jsString(value: String): String = buildString {
    append('"')
    for (c in value) when (c) {
        '"' -> append("\\\"")
        '\\' -> append("\\\\")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> if (c.code < 0x20 || c == '<' || c == '>' || c == '&' || c == '\'' || c.code == 0x2028 || c.code == 0x2029)
            append("\\u%04x".format(c.code)) else append(c)
    }
    append('"')
}
