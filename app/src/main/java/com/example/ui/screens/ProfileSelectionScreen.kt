package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Profile
import com.example.ui.viewmodel.MovieViewModel

// Avatar color palette for custom profiles (Prime-style bright accent tones)
val AvatarColors = listOf(
    Color(0xFF00A8E1), // Prime Blue
    Color(0xFFFF9900), // Amazon Orange
    Color(0xFFE50914), // Netflix Red
    Color(0xFF2BAD3B), // Green
    Color(0xFF9E00FF), // Violet Purple
    Color(0xFFE50D82)  // Bright Pink
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    viewModel: MovieViewModel,
    onProfileSelected: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    // Dialog state fields
    var newProfileName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableStateOf(0) }
    var isKidProfile by remember { mutableStateOf(false) }

    BackHandler(enabled = isEditMode || showAddProfileDialog) {
        if (showAddProfileDialog) {
            showAddProfileDialog = false
        } else if (isEditMode) {
            isEditMode = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F1E36), // Prime Video deep blue top
                        Color(0xFF09111E)  // Dark cinematic bottom
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
        ) {
            Text(
                text = if (isEditMode) "Administrar Perfiles" else "¿Quién está viendo?",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp).testTag("profile_screen_title")
            )

            // Grid Layout of Profiles
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 24.dp)
            ) {
                // List existing profiles
                items(profiles) { profile ->
                    val color = AvatarColors.getOrElse(profile.avatarColorIndex) { AvatarColors[0] }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("profile_item_${profile.name}")
                            .clickable {
                                if (isEditMode) {
                                    // Remove profile in edit mode if clicked
                                    viewModel.deleteProfile(profile)
                                } else {
                                    viewModel.selectProfile(profile)
                                    onProfileSelected()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isEditMode) 2.dp else 0.dp,
                                    color = if (isEditMode) Color.Red else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isEditMode) {
                                // Delete overlay banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Borrar",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                // Letter representation for profile avatar
                                Text(
                                    text = profile.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (profile.isKid) {
                            Text(
                                text = "Infantil",
                                color = Color(0xFF00A8E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .background(
                                        Color(0xFF00A8E1).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Plus option grid element to add new profiles (maximum of 6 profiles)
                if (profiles.size < 6) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .testTag("add_profile_card")
                                .clickable {
                                    newProfileName = ""
                                    selectedColorIndex = profiles.size % AvatarColors.size
                                    isKidProfile = false
                                    showAddProfileDialog = true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E2E4A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Añadir perfil",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Añadir Perfil",
                                color = Color.LightGray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Action button to enter edit mode
            Button(
                onClick = { isEditMode = !isEditMode },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditMode) Color(0xFF1A94FF) else Color(0xFF1E2E4A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
                    .testTag("edit_profiles_toggle_button")
            ) {
                Icon(
                    if (isEditMode) Icons.Default.Add else Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Listo" else "Administrar Perfiles",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Add Profile Dialog / Form
        if (showAddProfileDialog) {
            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false },
                containerColor = Color(0xFF1E2E4A),
                titleContentColor = Color.White,
                textContentColor = Color.LightGray,
                title = {
                    Text(
                        "Crear Nuevo Perfil",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            label = { Text("Nombre del Perfil", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00A8E1),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF00A8E1)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_profile_name_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            "Seleccionar Color de Avatar:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AvatarColors.forEachIndexed { index, color ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColorIndex == index) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorIndex = index }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Kids check Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isKidProfile = !isKidProfile }
                        ) {
                            Checkbox(
                                checked = isKidProfile,
                                onCheckedChange = { isKidProfile = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF00A8E1),
                                    checkmarkColor = Color.Black
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "¿Es un perfil infantil?",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Mostrará solo contenido familiar adecuado para niños.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newProfileName.trim().isNotEmpty()) {
                                viewModel.createProfile(newProfileName.trim(), selectedColorIndex, isKidProfile)
                                showAddProfileDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00A8E1),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAddProfileDialog = false }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}
