package com.project.speakmate.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.project.speakmate.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AuthRoute(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val manager = remember(context) { CredentialManager.create(context) }
    val clientId = stringResource(R.string.default_web_client_id)
    val scope = rememberCoroutineScope()
    val signIn =
        remember(context, manager, clientId, scope, viewModel) {
            {
                if (viewModel.startSignIn()) {
                    scope.launch {
                        try {
                            val request =
                                GetCredentialRequest.Builder()
                                    .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
                                    .build()
                            val credential = manager.getCredential(context, request).credential
                            check(
                                credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                            )
                            val token = GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.handleGoogleSignIn(
                                idToken = token.idToken,
                                displayName = token.displayName,
                                email = token.id,
                                photoUrl = token.profilePictureUri?.toString()
                            )
                        } catch (_: GetCredentialCancellationException) {
                            viewModel.cancelSignIn()
                        } catch (cancelled: CancellationException) {
                            viewModel.cancelSignIn()
                            throw cancelled
                        } catch (error: Exception) {
                            viewModel.showSignInError(error)
                        }
                    }
                }
                Unit
            }
        }
    GoogleSignInScreen(
        state = state,
        onSignIn = signIn,
        onOpenTerms = viewModel::showTerms,
        onOpenPrivacyPolicy = viewModel::showPrivacyPolicy,
        onRetryProfile = viewModel::retryProfile,
        onSignOut = viewModel::signOut,
        onDismissLegalInformation = viewModel::dismissLegalInformation,
        modifier = modifier
    )
}
