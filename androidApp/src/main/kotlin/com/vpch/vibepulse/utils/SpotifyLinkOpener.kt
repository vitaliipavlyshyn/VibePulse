import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.vpch.vibepulse.utils.SpotifyLinksConfig

internal fun openTrackInSpotifyOrBrowser(context: Context, spotifyUri: String) {
    val trackId = spotifyUri
        .substringAfterLast(":")
        .substringAfterLast("/")
        .substringBefore("?")
        .trim()

    val appUri = if (spotifyUri.startsWith("spotify:")) {
        spotifyUri.toUri()
    } else {
        "${SpotifyLinksConfig.trackUriPrefix}$trackId".toUri()
    }

    val webUri = if (spotifyUri.startsWith("http")) {
        spotifyUri.toUri()
    } else {
        "${SpotifyLinksConfig.openTrackBaseUrl}$trackId".toUri()
    }

    try {
        val spotifyIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage(SpotifyLinksConfig.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(spotifyIntent)
    } catch (_: ActivityNotFoundException) {
        val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    }
}