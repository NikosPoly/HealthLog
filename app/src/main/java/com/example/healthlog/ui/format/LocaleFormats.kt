package com.example.healthlog.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.healthlog.R
import java.util.Locale

// The number and date conventions of the language the app is shown in.
// They come from strings.xml, so they always match the displayed texts:
// a phone in a language the app doesn't support shows English texts,
// and gets the English formats too.

// The decimal separator: "," in Greek, "." in English.
@Composable
fun decimalSeparator(): Char = stringResource(R.string.decimal_separator).first()

// The language used for words inside dates, e.g. month names and AM/PM.
@Composable
fun formatLocale(): Locale = Locale.forLanguageTag(stringResource(R.string.format_locale))
