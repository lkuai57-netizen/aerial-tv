package app.orchard.tv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.orchard.tv.guard.HomeGuardService
import app.orchard.tv.guard.RevivalAccountService
import app.orchard.tv.update.Updates
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class BuildIdentityTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun homeComponentKeepsItsClassNameUnderTheInstalledPackage() {
        val expected = if (BuildConfig.MITV_HOME) "com.ts.launcher" else "app.orchard.tv"
        assertEquals(expected, context.packageName)
        val component = ComponentName(context, MainActivity::class.java)
        val info = context.packageManager.getActivityInfo(component, 0)
        assertEquals(expected, info.packageName)
        assertEquals("app.orchard.tv.MainActivity", info.name)
        val homes = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        assertTrue(homes.any { it.activityInfo.packageName == expected && it.activityInfo.name == info.name })
        assertEquals(expected + ".files",
            context.packageManager.resolveContentProvider(expected + ".files", 0)?.authority)
    }

    @Test fun guardAndAccountServicesBelongToThisCopy() {
        for (service in listOf(HomeGuardService::class.java, RevivalAccountService::class.java)) {
            assertEquals(context.packageName, context.packageManager.getServiceInfo(ComponentName(context, service), 0).packageName)
        }
        for (resource in listOf(R.xml.revival_account, R.xml.revival_sync)) {
            context.resources.getXml(resource).use { xml ->
                while (xml.eventType != XmlPullParser.START_TAG) xml.next()
                val id = xml.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "accountType", 0)
                assertTrue(id != 0)
                assertEquals(context.packageName, context.getString(id))
            }
        }
    }

    @Test fun renamedCopyCannotInstallAnOfficialPackage() {
        assertEquals(!BuildConfig.MITV_HOME, Updates.canInstall(context))
    }

    @Test fun renamedCopyIgnoresEvenAPreviouslyCachedUpstreamUpdate() {
        assumeTrue(BuildConfig.MITV_HOME)
        context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().putString("latest", "9999").commit()
        Updates.init(context)
        Updates.checkIfStale(force = true)
        assertNull(Updates.available)
        assertTrue(Updates.notes.isEmpty())
    }
}
