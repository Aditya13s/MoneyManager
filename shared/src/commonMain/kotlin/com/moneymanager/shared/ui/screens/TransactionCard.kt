package com.moneymanager.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.shared.model.Transaction
import com.moneymanager.shared.model.TransactionType
import com.moneymanager.shared.ui.theme.ExpenseColor
import com.moneymanager.shared.ui.theme.IncomeColor
import com.moneymanager.shared.util.badgeColor
import com.moneymanager.shared.util.emoji
import com.moneymanager.shared.util.formatCurrency
import com.moneymanager.shared.util.formatDate
import com.moneymanager.shared.util.toCategoryTitle

private const val HIDDEN_AMOUNT = "••••••"

@Composable
fun TransactionCard(
    transaction: Transaction,
    amountsHidden: Boolean = false,
    onClick: () -> Unit
) {
    val amountColor = if (transaction.type == TransactionType.EXPENSE) ExpenseColor else IncomeColor
    val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(transaction.category.badgeColor().copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(transaction.category.emoji(), fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${transaction.category.name.toCategoryTitle()} • ${formatDate(transaction.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (amountsHidden) "$sign $HIDDEN_AMOUNT" else "$sign${formatCurrency(transaction.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
