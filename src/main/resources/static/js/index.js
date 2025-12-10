// Load themes on page load
document.addEventListener('DOMContentLoaded', async () => {
    await loadThemes();
    setupCreateRoomForm();
    setupWordProviderToggle();
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
    const customThemeGroup = document.getElementById('customThemeGroup');
    
    radioButtons.forEach(radio => {
        radio.addEventListener('change', (e) => {
            if (e.target.value === 'ai') {
                customThemeGroup.style.display = 'block';
            } else {
                customThemeGroup.style.display = 'none';
                document.getElementById('customTheme').value = '';
            }
        });
    });
}

function setupCreateRoomForm() {
    const form = document.getElementById('createRoomForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const wordProvider = document.querySelector('input[name="wordProvider"]:checked').value;
        const theme = document.getElementById('theme').value;
        const customTheme = document.getElementById('customTheme').value.trim();
        
        // Validation
        if (wordProvider === 'ai' && customTheme) {
            // Custom theme for AI
            await createRoom(customTheme, wordProvider, true);
        } else if (theme) {
            // Standard theme from list
            await createRoom(theme, wordProvider, false);
        } else {
            showError('Пожалуйста, выберите тему из списка или введите свою');
            return;
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

