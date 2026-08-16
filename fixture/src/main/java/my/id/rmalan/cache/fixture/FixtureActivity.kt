package my.id.rmalan.cache.fixture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class FixtureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fixture = SafetyTestFixture(this)

        setContent {
            MaterialTheme {
                FixtureScreen(
                    fixture = fixture
                )
            }
        }
    }
}

@Composable
fun FixtureScreen(
    fixture: SafetyTestFixture,
    modifier: Modifier = Modifier
) {
    var status by remember { mutableStateOf(fixture.verifyState()) }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "CacheSweep Safety Fixture",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fixture Internal State", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cache Files Count:")
                        Text("${status.cacheFilesCount}")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cache Size:")
                        Text("${status.cacheBytes} bytes (~${status.cacheBytes / (1024 * 1024)} MB)")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SharedPreferences Intact:")
                        Text(if (status.prefsIntact) "YES (INTACT)" else "NO / MISSING")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Files (JSON) Intact:")
                        Text(if (status.filesIntact) "YES (INTACT)" else "NO / MISSING")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SQLite Database Intact:")
                        Text(if (status.dbIntact) "YES (INTACT)" else "NO / MISSING")
                    }
                }
            }

            Button(
                onClick = {
                    status = fixture.populateTestData()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Generate Test Data & Cache (~20 MB)")
            }

            OutlinedButton(
                onClick = {
                    status = fixture.verifyState()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh & Verify State")
            }
        }
    }
}
