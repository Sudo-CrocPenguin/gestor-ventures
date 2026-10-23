package com.gestor_ventures.back.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gestor_ventures.MainActivity
import com.gestor_ventures.R

/**
 * HU-26. Muestra las notificaciones de la app.
 *
 * HU-41 va a agregar más tipos (alertas del asistente, por ejemplo); cada uno tendrá su canal,
 * que es lo que deja al usuario apagar unos y dejar otros desde los ajustes del teléfono.
 */
object Notificador {

    const val CanalAgenda = "agenda"

    /** HU-38. Un canal aparte para que las alertas se puedan apagar sin apagar los recordatorios. */
    const val CanalAlertas = "alertas"

    /** Desde Android 8 una notificación necesita un canal. Se crea al abrir la app. */
    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CanalAgenda,
            context.getString(R.string.notificacion_canal_agenda),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notificacion_canal_agenda_desc) }
        val alertas = NotificationChannel(
            CanalAlertas,
            context.getString(R.string.notificacion_canal_alertas),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notificacion_canal_alertas_desc) }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(canal, alertas))
    }

    /** Desde Android 13 el usuario tiene que dar permiso; antes, siempre se puede. */
    fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Muestra la notificación. Tocarla abre la app. Sin permiso no hace nada: el recordatorio
     * queda guardado, pero no hay cómo avisar.
     */
    fun mostrar(context: Context, id: Int, canal: String, titulo: String, texto: String) {
        if (!puedeNotificar(context) || !PreferenciasNotificaciones(context).permite(canal)) return

        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (_: SecurityException) {
        }
    }
}
