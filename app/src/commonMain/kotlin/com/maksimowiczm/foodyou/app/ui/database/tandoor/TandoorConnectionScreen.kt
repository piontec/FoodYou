package com.maksimowiczm.foodyou.app.ui.database.tandoor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.food.infrastructure.tandoor.TandoorConnectionError
import foodyou.app.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TandoorConnectionScreen(
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TandoorConnectionViewModel = koinViewModel()
    val hasCredentials by viewModel.hasCredentials.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()

    TandoorConnectionScreen(
        onBack = onBack,
        onBrowse = onBrowse,
        hasCredentials = hasCredentials,
        state = state,
        onTestConnection = viewModel::testConnection,
        onSave = viewModel::save,
        onDisconnect = viewModel::disconnect,
        modifier = modifier,
    )
}

@Composable
internal fun TandoorConnectionScreen(
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    hasCredentials: Boolean,
    state: TandoorConnectionState,
    onTestConnection: (serverUrl: String, apiToken: String) -> Unit,
    onSave: (serverUrl: String, apiToken: String) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var serverUrl by rememberSaveable { mutableStateOf("") }
    var apiToken by rememberSaveable { mutableStateOf("") }

    val successMsg = stringResource(Res.string.info_tandoor_connection_success)
    val authErrorMsg = stringResource(Res.string.error_tandoor_auth)
    val reachabilityErrorMsg = stringResource(Res.string.error_tandoor_reachability)

    LaunchedEffect(state) {
        when (state) {
            TandoorConnectionState.Success -> {
                onSave(serverUrl, apiToken)
                scope.launch {
                    snackbarHostState.showSnackbar(successMsg)
                }
            }
            is TandoorConnectionState.Error -> {
                val msg = when (state.error) {
                    is TandoorConnectionError.AuthError -> authErrorMsg
                    is TandoorConnectionError.ReachabilityError -> reachabilityErrorMsg
                }
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(Res.string.headline_tandoor)) },
                navigationIcon = { ArrowBackIconButton(onBack) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.description_tandoor_token_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(stringResource(Res.string.headline_tandoor_server_url)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = apiToken,
                onValueChange = { apiToken = it },
                label = { Text(stringResource(Res.string.headline_tandoor_api_token)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { onTestConnection(serverUrl, apiToken) },
                modifier = Modifier.fillMaxWidth(),
                enabled = serverUrl.isNotBlank() && apiToken.isNotBlank() &&
                    state != TandoorConnectionState.Testing,
            ) {
                if (state == TandoorConnectionState.Testing) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(Res.string.action_tandoor_test_connection))
                }
            }

            if (hasCredentials) {
                OutlinedButton(
                    onClick = onBrowse,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.action_tandoor_browse_recipes))
                }

                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.action_tandoor_disconnect))
                }
            }
        }
    }
}
