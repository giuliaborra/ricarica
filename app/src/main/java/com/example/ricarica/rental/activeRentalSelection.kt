package com.example.ricarica.rental
import android.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActiveRentalsSection(
    isExpanded: Boolean,
    activeRentals: List<Rental>,
    onTerminateClick: (Rental) -> Unit
) {
    if (activeRentals.isEmpty()) return

    if (isExpanded) {

        Column(modifier = Modifier.fillMaxWidth()) {

            // Titolo sezione (opzionale)
            Text(
                text = "I tuoi noleggi (${activeRentals.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Pager per scorrere le card
            val pagerState = rememberPagerState(pageCount = { activeRentals.size })

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp), // Mostra un pezzetto della card successiva
                pageSpacing = 8.dp
            ) { page ->
                // Passiamo l'oggetto Rental corrente
                ActiveRentalCard(
                    rental = activeRentals[page],
                    onTerminateClick = onTerminateClick
                )
            }

            // Indicatore di pagina (i pallini sotto), utile solo se > 1
            if (activeRentals.size > 1) {
                Row(
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(activeRentals.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outlineVariant

                        Surface(
                            modifier = Modifier
                                .padding(2.dp)
                                .size(8.dp),
                            shape = MaterialTheme.shapes.small,
                            color = color
                        ) {}
                    }
                }
            }
        }
    }
    else {

        CollapsedActiveRentalBar(rentals = activeRentals)

    }
}