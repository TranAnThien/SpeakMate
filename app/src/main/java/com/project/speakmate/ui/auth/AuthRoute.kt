package com.project.speakmate.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.project.speakmate.R
import com.project.speakmate.ui.auth.state.AuthEffect
import com.project.speakmate.ui.auth.state.GoogleSignInStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AuthRoute(
    onSignedIn: () -> Unit,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val manager = remember(context) { CredentialManager.create(context) }
    val clientId = stringResource(R.string.default_web_client_id)
    val request = remember(clientId) {
        GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
    }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val signedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    AuthEffect.SignedIn -> signedIn()
                }
            }
        }
    }
    GoogleSignInScreen(
        state = state,
        onSignIn = {
            if (viewModel.uiState.value.status != GoogleSignInStatus.LOADING) {
                viewModel.startSignIn()
                coroutineScope.launch {
                    try {
                        val credential = manager.getCredential(context, request).credential
                        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                        val token = GoogleIdTokenCredential.createFrom(credential.data)
                        viewModel.handleGoogleSignIn(token.idToken, token.displayName, token.id, token.profilePictureUri?.toString())
                    } catch (_: GetCredentialCancellationException) {
                        viewModel.onGoogleSignInCancelled()
                    } catch (cancelled: CancellationException) {
                        viewModel.onGoogleSignInCancelled()
                        throw cancelled
                    } catch (error: Exception) {
                        viewModel.onGoogleSignInError(error)
                    }
                }
            }
        },
        onOpenTerms = {},
        onOpenPrivacyPolicy = {},
        modifier = modifier
    )
}
