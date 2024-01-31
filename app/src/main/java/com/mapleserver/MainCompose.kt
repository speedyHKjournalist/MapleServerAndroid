import android.app.PendingIntent
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mapleserver.LogViewModel
import com.mapleserver.SharedUtil
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.StartButton
import com.mapleserver.ui.theme.StopButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun MainCompose(context: Context, navController: NavHostController, notificationPendingIntent: PendingIntent) {
    val logView = LogViewModel(LocalContext.current)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isStartButtonEnabled = rememberSaveable { mutableStateOf(true) }
    var isStopButtonEnabled = rememberSaveable { mutableStateOf(false) }

    MapleServerTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text("OpenMapleServer", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                    Divider()
                    NavigationDrawerItem(
                        label = { Text(text = "ServerConfig") },
                        selected = false,
                        onClick = { navController.navigate("config_editor_screen") }
                    )
                    NavigationDrawerItem(
                        label = { Text(text = "ImportDatabase") },
                        selected = false,
                        onClick = { navController.navigate("import_db_screen") }
                    )
                    NavigationDrawerItem(
                        label = { Text(text = "ExportDatabase") },
                        selected = false,
                        onClick = { navController.navigate("export_db_screen") }
                    )
                }
            }
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DrawerToggleButton(drawerState, scope)
                        StartButton(
                            text = "Start",
                            isButtonEnabled = isStartButtonEnabled,
                            startMapleServer = {
                                isStartButtonEnabled.value = false
                                isStopButtonEnabled.value = true
                                SharedUtil.startMapleServer(context, notificationPendingIntent)
                            }
                        )
                        StopButton(
                            text = "Stop",
                            isButtonEnabled = isStopButtonEnabled,
                            stopMapleServer = {
                                isStartButtonEnabled.value = true
                                isStopButtonEnabled.value = false
                                SharedUtil.stopMapleServer(context)
                            }
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .background(Color.White)
                                    .padding(11.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = logView.logMessage.value,
                                    modifier = Modifier.padding(11.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerToggleButton(drawerState: DrawerState, scope: CoroutineScope) {
    // Add a button to toggle the drawer
    IconButton(
        onClick = {
            scope.launch {
                drawerState.apply {
                    if (isClosed) open() else close()
                }
            }
        }) {
        Icon(imageVector = Icons.Default.Menu, contentDescription = "Drawer Toggle Button")
    }
}