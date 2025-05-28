/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal.ui

/**
 * Tags used to identify UI elements in Assurance UI tests.
 */
internal object AssuranceUiTestTags {
    internal const val ASSURANCE_HEADER = "assuranceHeader"
    internal const val ASSURANCE_SUB_HEADER = "assuranceSubHeader"

    internal object QuickConnectScreen {
        internal const val QUICK_CONNECT_VIEW = "quickConnectView"
        internal const val QUICK_CONNECT_SCROLLVIEW = "quickConnectScrollView"
        internal const val QUICK_CONNECT_LOGO = "quickConnectLogo"
        internal const val ADOBE_LOGO = "adobeLogo"

        internal const val PROGRESS_INDICATOR = "progressIndicator"
        internal const val PROGRESS_BUTTON = "progressButton"
        internal const val PROGRESS_BUTTON_TEXT = "progressButtonText"
        internal const val CANCEL_BUTTON = "cancelButton"
        internal const val CONNECTION_ERROR_PANEL = "connectionErrorPanel"
        internal const val CONNECTION_ERROR_TEXT = "connectionErrorText"
        internal const val CONNECTION_ERROR_DESCRIPTION = "connectionErrorDescription"
    }

    internal object PinScreen {
        internal const val DIAL_PAD_VIEW = "dialPadView"
        internal const val NUMBER_ROW = "dialPadRow"
        internal const val SYMBOL_ROW = "symbolRow"
        internal const val DIAL_PAD_BUTTON = "dialPadButton"
        internal const val DIAL_PAD_DELETE_BUTTON = "dialPadDeleteButton"
        internal const val DIAL_PAD_NUMERIC_BUTTON_TEXT = "dialPadNumericButton"
        internal const val INPUT_FEEDBACK_ROW = "inputFeedbackRow"
        internal const val DIAL_PAD_ACTION_BUTTON_ROW = "dialPadActionButtonRow"
        internal const val DIAL_PAD_CANCEL_BUTTON = "dialPadCancelButton"
        internal const val DIAL_PAD_CONNECT_BUTTON = "dialPadConnectButton"

        internal const val PIN_ERROR_VIEW = "pinErrorView"
        internal const val PIN_ERROR_HEADER = "pinErrorHeader"
        internal const val PIN_ERROR_CONTENT = "pinErrorContent"
        internal const val PIN_ERROR_ACTION_BUTTON_ROW = "pinErrorActionButtonRow"
        internal const val PIN_ERROR_CANCEL_BUTTON = "pinErrorCancelButton"
        internal const val PIN_ERROR_RETRY_BUTTON = "pinErrorRetryButton"

        internal const val PIN_CONNECTING_VIEW = "pinConnectingView"
        internal const val PIN_CONNECTING_LOADING_INDICATOR = "pinConnectingLoadingIndicator"
    }

    internal object ErrorScreen {
        internal const val ERROR_VIEW = "errorView"
        internal const val ERROR_TITLE = "errorTitle"
        internal const val ERROR_DESCRIPTION = "errorDescription"
        internal const val DISMISS_BUTTON = "dismissButton"
    }

    internal object StatusScreen {
        internal const val STATUS_VIEW = "statusView"
        internal const val LOGS_PANEL = "logsPanel"
        internal const val LOGS_CONTENT = "logsContent"
        internal const val LOG_ENTRY = "logEntry"
        internal const val STATUS_CLOSE_BUTTON = "statusCloseButton"
        internal const val CLEAR_LOG_BUTTON = "clearLogButton"
        internal const val STATUS_DISCONNECT_BUTTON = "statusDisconnectButton"
    }
}
