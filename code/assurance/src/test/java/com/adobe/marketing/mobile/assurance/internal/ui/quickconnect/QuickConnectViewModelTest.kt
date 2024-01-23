package com.adobe.marketing.mobile.assurance.internal.ui.quickconnect

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.mobile.assurance.AssuranceComponentRegistry
import com.adobe.marketing.mobile.assurance.AssuranceConstants
import com.adobe.marketing.mobile.assurance.AssuranceStateManager
import com.adobe.marketing.mobile.assurance.QuickConnectManager
import com.adobe.marketing.mobile.assurance.SessionUIOperationHandler
import com.adobe.marketing.mobile.assurance.internal.ui.common.ConnectionState
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class QuickConnectViewModelTest {

    @Mock
    private lateinit var mockQuickConnectManager: QuickConnectManager

    @Mock
    private lateinit var mockAssuranceStateManager: AssuranceStateManager

    @Mock
    private lateinit var mockSessionUIOperationHandler: SessionUIOperationHandler

    private lateinit var quickConnectViewModel: QuickConnectViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Setup tests with a mock AssuranceComponentRegistry
        AssuranceComponentRegistry.initialize(
            mockAssuranceStateManager,
            mockSessionUIOperationHandler
        )
    }

    @Test
    fun `Test that Connect action on UI registers the device session`() {
        // Setup quick connect view model with current state of disconnected
        val mockState: MutableState<ConnectionState> =
            mutableStateOf(ConnectionState.Disconnected(null))
        quickConnectViewModel = QuickConnectViewModel(
            mockState,
            mockQuickConnectManager,
            AssuranceConstants.AssuranceEnvironment.PROD
        )

        // Test
        quickConnectViewModel.onAction(QuickConnectScreenAction.Connect)

        // Verify
        Assert.assertEquals(
            ConnectionState.Connecting,
            quickConnectViewModel.state.value
        )
        verify(mockQuickConnectManager).registerDevice()
    }

    @Test
    fun `Test that Cancel action on UI disconnects the session`() {
        // Setup quick connect view model with current state of Connecting
        val mockState: MutableState<ConnectionState> = mutableStateOf(ConnectionState.Connecting)
        quickConnectViewModel = QuickConnectViewModel(
            mockState,
            mockQuickConnectManager,
            AssuranceConstants.AssuranceEnvironment.PROD
        )

        // Test
        quickConnectViewModel.onAction(QuickConnectScreenAction.Cancel)

        // Verify
        Assert.assertEquals(
            ConnectionState.Disconnected(null),
            quickConnectViewModel.state.value
        )
        verify(mockSessionUIOperationHandler).onCancel()
    }

    @Test
    fun `Test that Retry action on UI transitions to Connecting state`() {
        // Setup quick connect view model with current state of Disconnected with an error
        val mockState: MutableState<ConnectionState> =
            mutableStateOf(ConnectionState.Disconnected(AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR))
        quickConnectViewModel = QuickConnectViewModel(
            mockState,
            mockQuickConnectManager,
            AssuranceConstants.AssuranceEnvironment.PROD
        )

        // Test
        quickConnectViewModel.onAction(QuickConnectScreenAction.Retry)

        // Verify
        Assert.assertEquals(
            ConnectionState.Connecting,
            quickConnectViewModel.state.value
        )
    }

    @Test
    fun `Test that onSessionConnected transitions to Connected state`() {
        val mockState: MutableState<ConnectionState> = mutableStateOf(ConnectionState.Connecting)
        val statusListenerWrapper = AssuranceStatusListenerWrapper(mockState)
        quickConnectViewModel = QuickConnectViewModel(
            mockState,
            statusListenerWrapper,
            mockAssuranceStateManager,
            AssuranceConstants.AssuranceEnvironment.PROD
        )

        // Test
        statusListenerWrapper.onSessionConnected()

        // Verify
        Assert.assertEquals(
            ConnectionState.Connected,
            quickConnectViewModel.state.value
        )
    }

    @Test
    fun `Test that onSessionDisconnected transitions to Disconnected state`() {
        val mockState: MutableState<ConnectionState> = mutableStateOf(ConnectionState.Connecting)
        val statusListenerWrapper = AssuranceStatusListenerWrapper(mockState)
        quickConnectViewModel = QuickConnectViewModel(
            mockState,
            statusListenerWrapper,
            mockAssuranceStateManager,
            AssuranceConstants.AssuranceEnvironment.PROD
        )

        // Test
        statusListenerWrapper.onSessionDisconnected(AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR)

        // Verify
        Assert.assertEquals(
            ConnectionState.Disconnected(AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR),
            quickConnectViewModel.state.value
        )
    }

    @Test
    fun `Test that onSessionTerminated transitions state to Disconnected`() {
        val mockState: MutableState<ConnectionState> = mutableStateOf(ConnectionState.Connecting)
        val statusListenerWrapper = AssuranceStatusListenerWrapper(mockState)
        quickConnectViewModel = QuickConnectViewModel(
            mockState,
            statusListenerWrapper,
            mockAssuranceStateManager,
            AssuranceConstants.AssuranceEnvironment.PROD
        )

        // Test
        statusListenerWrapper.onSessionTerminated(AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR)

        // Verify
        Assert.assertEquals(
            ConnectionState.Disconnected(AssuranceConstants.AssuranceConnectionError.UNEXPECTED_ERROR),
            quickConnectViewModel.state.value
        )
    }

    @After
    fun tearDown() {
    }
}
