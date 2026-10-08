package com.cleanui.echo.extensions.exceptions

class RequiredExtensionsMissingException(val required: List<String>) :
    Exception("Missing required extensions: ${required.joinToString(", ")}")