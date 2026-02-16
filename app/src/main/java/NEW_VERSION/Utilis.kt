import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.MapsInitializer // <--- Importante
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun bitmapDescriptorFromVector(
    context: Context,
    vectorResId: Int
): BitmapDescriptor? {
    return try {
        // 1. SVEGLIA GOOGLE MAPS MANUALMENTE
        // Questo risolve l'errore "IBitmapDescriptorFactory is not initialized"
        MapsInitializer.initialize(context)

        // 2. Carica il drawable
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        if (vectorDrawable == null) {
            Log.e("MapError", "Risorsa non trovata ID: $vectorResId")
            return null
        }

        // 3. Gestisci le dimensioni (fallback se 0)
        var w = vectorDrawable.intrinsicWidth
        var h = vectorDrawable.intrinsicHeight
        if (w <= 0 || h <= 0) {
            w = 64 // Dimensione di default se l'XML non ne ha
            h = 64
        }

        // 4. Disegna su Canvas
        vectorDrawable.setBounds(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)

        // 5. Restituisci il BitmapDescriptor
        BitmapDescriptorFactory.fromBitmap(bitmap)

    } catch (e: Exception) {
        Log.e("MapError", "Errore creazione icona marker", e)
        // Ritorna null così la mappa userà il marker rosso di default invece di crashare
        null
    }
}