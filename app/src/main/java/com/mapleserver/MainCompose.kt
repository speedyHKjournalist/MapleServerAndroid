import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.mapleserver.LogViewModel
import com.mapleserver.MainViewModel
import com.mapleserver.ServerConfig
import com.mapleserver.SharedUtil
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.StartButton
import com.mapleserver.ui.theme.StopButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File


@Composable
fun MainCompose(context: Context, navController: NavHostController, mainViewModel: MainViewModel) {
    val logView = LogViewModel(LocalContext.current)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    checkIfEnabled(context, mainViewModel.connection, mainViewModel.serviceIntent)

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
                            isButtonEnabled = mainViewModel.isStartButtonEnabled,
                            startMapleServer = {
                                mainViewModel.isStartButtonEnabled.value = false
                                mainViewModel.isStopButtonEnabled.value = true
                                SharedUtil.startMapleServer(
                                    context,
                                    mainViewModel.serviceIntent,
                                    mainViewModel.notificationPendingIntent
                                )
                            }
                        )
                        StopButton(
                            text = "Stop",
                            isButtonEnabled = mainViewModel.isStopButtonEnabled,
                            stopMapleServer = {
                                mainViewModel.isStartButtonEnabled.value = true
                                mainViewModel.isStopButtonEnabled.value = false
                                SharedUtil.stopMapleServer(
                                    context,
                                    mainViewModel.serviceIntent,
                                    mainViewModel.connection
                                )
                            }
                        )
                    }
                    DisplayCurrentIP(mainViewModel)
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
fun DisplayCurrentIP(mainViewModel: MainViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "WAN_IP: ${mainViewModel.serverConfig.server.HOST}",
            modifier = Modifier.padding(9.dp),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "LAN_IP: ${mainViewModel.serverConfig.server.LANHOST}",
            modifier = Modifier.padding(9.dp),
            fontWeight = FontWeight.Bold
        )
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

fun checkIfEnabled(
    context: Context,
    connection: ServiceConnection,
    serviceIntent: Intent
) {
    context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
}