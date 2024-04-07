import android.content.*
import android.content.Context.RECEIVER_NOT_EXPORTED
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.navigation.NavHostController
import com.mapleserver.LogViewModel
import com.mapleserver.MainViewModel
import com.mapleserver.ServerService
import com.mapleserver.ui.theme.MapleServerTheme
import com.mapleserver.ui.theme.StartButton
import com.mapleserver.ui.theme.StopButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun MainCompose(context: Context, navController: NavHostController, mainViewModel: MainViewModel) {
    val logView = LogViewModel(LocalContext.current)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val intentFilter = remember { IntentFilter("MapleServerMessage") }

    val showProcessingBar = remember { mutableStateOf(false) }
    val isStartButtonEnabled = remember { mutableStateOf(false) }
    val isStopButtonEnabled = remember { mutableStateOf(true) }
    val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val status = intent?.getStringExtra("Status")

            if (status == "ServerStarting") {
                showProcessingBar.value = true
            } else if (status == "ServerStarted") {
                showProcessingBar.value = false
                isStartButtonEnabled.value = false
                isStopButtonEnabled.value = true
            } else if (status == "ServerStopping") {
                showProcessingBar.value = true
            } else if (status == "ServerStopped") {
                showProcessingBar.value = false
                isStartButtonEnabled.value = true
                isStopButtonEnabled.value = false
            }
        }
    }

    val mConnection: ServiceConnection = object : ServiceConnection {
        lateinit var myService: ServerService

        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            val binder: ServerService.LocalBinder = service as ServerService.LocalBinder
            myService = binder.getService()

            isStartButtonEnabled.value = !myService.isRunning()
            isStopButtonEnabled.value = !isStartButtonEnabled.value
        }

        override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    doBindService(context, mConnection, mainViewModel.serviceIntent)

    DisposableEffect(key1 = true) {
        context.registerReceiver(statusReceiver, intentFilter, RECEIVER_NOT_EXPORTED)

        onDispose {
            context.unregisterReceiver(statusReceiver)
        }
    }

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
                                startMapleServer(
                                    context,
                                    mainViewModel.serviceIntent
                                )
                            }
                        )
                        StopButton(
                            text = "Stop",
                            isButtonEnabled = isStopButtonEnabled,
                            stopMapleServer = {
                                stopMapleServer(
                                    context,
                                    mainViewModel.serviceIntent,
                                    mConnection
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
                DisplayProcessingBar(showProcessingBar)
            }
        }
    }
}

@Composable
fun DisplayProcessingBar(showProcessingBar: MutableState<Boolean>) {
    if (showProcessingBar.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(16.dp)
                    .size(48.dp)
            )
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

fun doBindService(
    context: Context,
    connection: ServiceConnection,
    serviceIntent: Intent
) {
    context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
}

fun startMapleServer(context: Context, serviceIntent : Intent) {
    startForegroundService(context, serviceIntent)
}

fun stopMapleServer(context: Context, serviceIntent : Intent, connection : ServiceConnection) {
    context.unbindService(connection)
    context.stopService(serviceIntent)
}