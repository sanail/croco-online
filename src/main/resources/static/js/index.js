// Load themes on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadThemes();
    setupCreateRoomForm();
    setupWordProviderToggle();
    setupAiThemeSourceToggle();
});

async function loadThemes() {
    try {
        const response = await fetch('/api/rooms/themes');
        const themes = await response.json();
        
        const themeSelect = document.getElementById('theme');
        themes.forEach(theme => {
            const option = document.createElement('option');
            option.value = theme;
            option.textContent = theme;
            themeSelect.appendChild(option);
        });
    } catch (error) {
        console.error('Error loading themes:', error);
        showError('Не удалось загрузить темы');
    }
}

function setupWordProviderToggle() {
    const radioButtons = document.querySelectorAll('input[name="wordProvider"]');
    const aiThemeSourceGroup = document.getElementById('aiThemeSourceGroup');
    const themeSelectGroup = document.getElementById('themeSelectGroup');
    const customThemeGroup = document.getElementById('customThemeGroup');
    
    radioButtons.forEach(radio => {
        radio.addEventListener('change', (e) => {
            if (e.target.value === 'ai') {
                // Show AI theme source selector
                aiThemeSourceGroup.style.display = 'block';
                // Update visibility based on current AI theme source selection
                updateAiThemeVisibility();
            } else {
                // Database: show only theme select
                aiThemeSourceGroup.style.display = 'none';
                themeSelectGroup.style.display = 'block';
                customThemeGroup.style.display = 'none';
                // Make theme select required for database
                document.getElementById('theme').required = true;
                document.getElementById('customTheme').value = '';
            }
        });
    });
}

function setupAiThemeSourceToggle() {
    const aiThemeSourceRadios = document.querySelectorAll('input[name="aiThemeSource"]');
    
    aiThemeSourceRadios.forEach(radio => {
        radio.addEventListener('change', () => {
            updateAiThemeVisibility();
        });
    });
}

function updateAiThemeVisibility() {
    const aiThemeSource = document.querySelector('input[name="aiThemeSource"]:checked')?.value;
    const themeSelectGroup = document.getElementById('themeSelectGroup');
    const customThemeGroup = document.getElementById('customThemeGroup');
    const themeSelect = document.getElementById('theme');
    
    if (aiThemeSource === 'custom') {
        // Show custom theme input, hide theme select
        themeSelectGroup.style.display = 'none';
        customThemeGroup.style.display = 'block';
        themeSelect.required = false;
        themeSelect.value = '';
    } else {
        // Show theme select, hide custom input
        themeSelectGroup.style.display = 'block';
        customThemeGroup.style.display = 'none';
        themeSelect.required = true;
        document.getElementById('customTheme').value = '';
    }
}

function setupCreateRoomForm() {
    const form = document.getElementById('createRoomForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const wordProvider = document.querySelector('input[name="wordProvider"]:checked').value;
        
        if (wordProvider === 'database') {
            // Database: must select from list
            const theme = document.getElementById('theme').value;
            if (!theme) {
                showError('Пожалуйста, выберите тему из списка');
                return;
            }
            await createRoom(theme, wordProvider, false);
        } else {
            // AI: check which source is selected
            const aiThemeSource = document.querySelector('input[name="aiThemeSource"]:checked').value;
            
            if (aiThemeSource === 'custom') {
                // Custom theme: must enter text
                const customTheme = document.getElementById('customTheme').value.trim();
                if (!customTheme) {
                    showError('Пожалуйста, введите название темы');
                    return;
                }
                await createRoom(customTheme, wordProvider, true);
            } else {
                // Predefined theme: must select from list
                const theme = document.getElementById('theme').value;
                if (!theme) {
                    showError('Пожалуйста, выберите тему из списка');
                    return;
                }
                await createRoom(theme, wordProvider, false);
            }
        }
    });
}

async function createRoom(theme, wordProviderType, isCustomTheme) {
    try {
        const response = await fetch('/api/rooms', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                theme: theme,
                wordProviderType: wordProviderType,
                customTheme: isCustomTheme
            })
        });
        
        if (!response.ok) {
            throw new Error('Failed to create room');
        }
        
        const data = await response.json();
        showRoomLink(data.roomCode);
    } catch (error) {
        console.error('Error creating room:', error);
        showError('Не удалось создать комнату. Попробуйте ещё раз.');
    }
}

function showError(message) {
    const errorDiv = document.getElementById('error-message');
    errorDiv.textContent = message;
    errorDiv.classList.add('show');
    
    setTimeout(() => {
        errorDiv.classList.remove('show');
    }, 5000);
}

function showRoomLink(roomCode) {
    const roomLinkDiv = document.getElementById('room-link');
    const roomUrl = `${window.location.origin}/room/${roomCode}`;
    
    roomLinkDiv.innerHTML = `
        <h3>✅ Комната создана!</h3>
        <p>Поделитесь этой ссылкой с друзьями:</p>
        <p><a href="${roomUrl}" target="_blank">${roomUrl}</a></p>
        <button onclick="copyToClipboard('${roomUrl}')" class="btn btn-primary" style="margin-top: 10px;">
            Скопировать ссылку
        </button>
        <p style="margin-top: 15px;">
            <a href="${roomUrl}" class="btn btn-success">Перейти в комнату</a>
        </p>
    `;
    roomLinkDiv.classList.add('show');
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        alert('Ссылка скопирована в буфер обмена!');
    }).catch(err => {
        console.error('Failed to copy:', err);
    });
}

