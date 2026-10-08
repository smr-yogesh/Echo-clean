package com.cleanui.echo.extensions.exceptions

class ExtensionNotFoundException(val id: String?) : Exception("Extension not found: $id")