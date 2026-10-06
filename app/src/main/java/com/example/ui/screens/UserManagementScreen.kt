package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserAccount
import com.example.ui.components.tvFocusable
import com.example.ui.viewmodel.MovieViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class UserFilterTab {
    TODOS,
    PENDIENTES,
    APROBADOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: MovieViewModel
) {
    val context = LocalContext.current
    val allUsers by viewModel.allUsers.collectAsState()

    var selectedTab by remember { mutableStateOf(UserFilterTab.TODOS) }
    var searchQuery by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }

    val pendingCount = remember(allUsers) { allUsers.count { !it.isApproved && !it.isAdmin } }
    val approvedCount = remember(allUsers) { allUsers.count { it.isApproved || it.isAdmin } }

    val filteredUsers = remember(allUsers, selectedTab, searchQuery) {
        val query = searchQuery.trim().lowercase()
        allUsers.filter { user ->
            val matchesTab = when (selectedTab) {
                UserFilterTab.TODOS -> true
                UserFilterTab.PENDIENTES -> !user.isApproved && !user.isAdmin
                UserFilterTab.APROBADOS -> user.isApproved || user.isAdmin
            }
            val matchesSearch = query.isEmpty() ||
                    user.name.lowercase().contains(query) ||
                    user.email.lowercase().contains(query)
            matchesTab && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09111E))
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER BANNER
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("user_management_header_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF00A8E1).copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF00A8E1),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Gestión de Usuarios",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Panel de Administrador de la App",
                                        color = Color(0xFF00A8E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (pendingCount > 0) Color(0xFFFF9900) else Color(0xFF2BAD3B)
                            ) {
                                Text(
                                    text = if (pendingCount > 0) "$pendingCount Pendiente${if (pendingCount > 1) "s" else ""}" else "Al día",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Por política de acceso, los usuarios que se registran no pueden ingresar hasta que tú los apruebes. Puedes activar el acceso o eliminar cuentas en cualquier momento.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // STATS ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                title = "Total",
                                count = allUsers.size.toString(),
                                containerColor = Color(0xFF1E2E4A),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Pendientes",
                                count = pendingCount.toString(),
                                containerColor = if (pendingCount > 0) Color(0xFFFF9900).copy(alpha = 0.2f) else Color(0xFF1E2E4A),
                                textColor = if (pendingCount > 0) Color(0xFFFF9900) else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Aprobados",
                                count = approvedCount.toString(),
                                containerColor = Color(0xFF2BAD3B).copy(alpha = 0.2f),
                                textColor = Color(0xFF2BAD3B),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // SEARCH BAR & FILTER TABS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_search_input"),
                        placeholder = { Text("Buscar usuario por nombre o correo...", color = Color.Gray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Borrar", tint = Color.LightGray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F1E36),
                            unfocusedContainerColor = Color(0xFF0F1E36),
                            focusedBorderColor = Color(0xFF00A8E1),
                            unfocusedBorderColor = Color(0xFF1E2E4A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // TABS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1E36), shape = RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabFilterChip(
                            title = "Todos (${allUsers.size})",
                            isSelected = selectedTab == UserFilterTab.TODOS,
                            onClick = { selectedTab = UserFilterTab.TODOS },
                            modifier = Modifier.weight(1f)
                        )
                        TabFilterChip(
                            title = "Pendientes ($pendingCount)",
                            isSelected = selectedTab == UserFilterTab.PENDIENTES,
                            onClick = { selectedTab = UserFilterTab.PENDIENTES },
                            badgeColor = if (pendingCount > 0) Color(0xFFFF9900) else null,
                            modifier = Modifier.weight(1f)
                        )
                        TabFilterChip(
                            title = "Aprobados ($approvedCount)",
                            isSelected = selectedTab == UserFilterTab.APROBADOS,
                            onClick = { selectedTab = UserFilterTab.APROBADOS },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // USER CARDS LIST
            if (filteredUsers.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E36).copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No se encontraron usuarios para \"$searchQuery\"" else "No hay usuarios en esta categoría",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(filteredUsers, key = { it.id }) { user ->
                    UserCardItem(
                        user = user,
                        onApprove = {
                            viewModel.approveUser(user.id, true)
                            Toast.makeText(context, "Usuario ${user.name} aprobado y activado", Toast.LENGTH_SHORT).show()
                        },
                        onRevoke = {
                            viewModel.approveUser(user.id, false)
                            Toast.makeText(context, "Acceso revocado para ${user.name}", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteRequest = {
                            userToDelete = user
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // CONFIRMATION DIALOG FOR USER DELETION
    userToDelete?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = {
                Text(
                    text = "🗑️ ¿Eliminar Usuario?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Estás a punto de eliminar definitivamente la cuenta de:",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• ${targetUser.name} (${targetUser.email})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Esta acción eliminará la cuenta y todos sus perfiles asociados de forma permanente. El usuario ya no podrá iniciar sesión.",
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(targetUser)
                        Toast.makeText(context, "Cuenta de ${targetUser.name} eliminada", Toast.LENGTH_SHORT).show()
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Text("Eliminar definitivamente", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancelar", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF0F1E36)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    count: String,
    containerColor: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = title,
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TabFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) Color(0xFF00A8E1) else Color.Transparent,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else (badgeColor ?: Color.LightGray),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun UserCardItem(
    user: UserAccount,
    onApprove: () -> Unit,
    onRevoke: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val registrationDate = remember(user.createdAt) {
        if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "Registrado"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!user.isApproved && !user.isAdmin) Color(0xFF16233B) else Color(0xFF0F1E36)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_card_${user.id}")
            .border(
                width = 1.dp,
                color = if (!user.isApproved && !user.isAdmin) Color(0xFFFF9900).copy(alpha = 0.4f) else Color(0xFF1E2E4A),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (user.isAdmin) Color(0xFF00A8E1)
                            else if (user.isApproved) Color(0xFF2BAD3B)
                            else Color(0xFFFF9900)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (user.isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF00A8E1)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = user.email,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Registrado: $registrationDate",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                // STATUS BADGE
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        user.isAdmin -> Color(0xFF00A8E1).copy(alpha = 0.2f)
                        user.isApproved -> Color(0xFF2BAD3B).copy(alpha = 0.2f)
                        else -> Color(0xFFFF9900).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = when {
                            user.isAdmin -> "👑 Admin Principal"
                            user.isApproved -> "✓ Aprobado"
                            else -> "⏳ Pendiente"
                        },
                        color = when {
                            user.isAdmin -> Color(0xFF00A8E1)
                            user.isApproved -> Color(0xFF2BAD3B)
                            else -> Color(0xFFFF9900)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // ACTIONS (Only for non-admin accounts)
            if (!user.isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF1E2E4A), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!user.isApproved) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2BAD3B)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .tvFocusable(shape = RoundedCornerShape(6.dp))
                                .testTag("approve_user_${user.id}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aprobar y Activar Cuenta", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onRevoke,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9900)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9900)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.tvFocusable(shape = RoundedCornerShape(6.dp))
                        ) {
                            Text("Desactivar Acceso", color = Color(0xFFFF9900), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    IconButton(
                        onClick = onDeleteRequest,
                        modifier = Modifier
                            .tvFocusable(shape = RoundedCornerShape(20.dp), focusedScale = 1.15f)
                            .testTag("delete_user_${user.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar usuario",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
