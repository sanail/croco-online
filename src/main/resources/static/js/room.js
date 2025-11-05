let currentPlayerId = null;
let isLeader = false;
let pollingInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    setupJoinForm();
});

function setupJoinForm() {
    const form = document.getElementById('joinForm');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const playerName = document.getElementById('playerName').value.trim();
        
        if (playerName.length < 2) {
            showJoinError('Имя должно содержать минимум 2 символа');
            return;
        }
        
        await joinRoom(playerName);
    });
}

async function joinRoom(playerName) {
    try {
        const response = await fetch(`/api/rooms/${ROOM_CODE}/join`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                playerName: playerName
            })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to join room');
        }
        
        const data = await response.json();
        currentPlayerId = data.playerId;
        isLeader = data.isLeader;
        
        // Hide join form, show game
        document.getElementById('joinSection').style.display = 'none';
        document.getElementById('gameSection').style.display = 'block';
        
        // Start polling for room state
        startPolling();
        
        // Setup game controls
        setupGameControls();
    } catch (error) {
        console.error('Error joining room:', error);
        showJoinError('Не удалось присоединиться к комнате: ' + error.message);
    }
}

function setupGameControls() {
    // Generate word button
    const generateBtn = document.getElementById('generateWordBtn');
    if (generateBtn) {
        generateBtn.addEventListener('click', generateNewWord);
    }
    
    // Guess form
    const guessForm = document.getElementById('guessForm');
    if (guessForm) {
        guessForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const guess = document.getElementById('guess').value.trim();
            await submitGuess(guess);
        });
    }
    
    // Leave button
    const leaveBtn = document.getElementById('leaveBtn');
    if (leaveBtn) {
        leaveBtn.addEventListener('click', leaveRoom);
    }
}

async function loadRoomState() {
    try {
        const response = await fetch(`/api/rooms/${ROOM_CODE}/state`);
        
        if (!response.ok) {
            throw new Error('Failed to load room state');
        }
        
        const data = await response.json();
        updateUI(data);
    } catch (error) {
        console.error('Error loading room state:', error);
    }
}

function updateUI(roomState) {
    // Update theme
    document.getElementById('theme').textContent = roomState.theme;
    
    // Update current leader
    document.getElementById('currentLeader').textContent = roomState.currentLeaderName || 'Нет';
    
    // Update players list
    updatePlayersList(roomState.players);
    
    // Find current player
    const currentPlayer = roomState.players.find(p => p.id === currentPlayerId);
    if (!currentPlayer) return;
    
    isLeader = currentPlayer.isLeader;
    
    // Show appropriate view
    if (isLeader) {
        document.getElementById('leaderView').style.display = 'block';
        document.getElementById('playerView').style.display = 'none';
        
        // Update word display
        if (roomState.currentWord) {
            document.getElementById('wordDisplay').textContent = roomState.currentWord;
        } else {
            document.getElementById('wordDisplay').textContent = 'Нажмите кнопку для генерации слова';
        }
        
        // Update assign winner buttons
        updateWinnerButtons(roomState.players);
    } else {
        document.getElementById('leaderView').style.display = 'none';
        document.getElementById('playerView').style.display = 'block';
        
        // Show/hide guess form based on whether word exists
        if (roomState.hasWord) {
            document.getElementById('waitingMessage').style.display = 'none';
            document.getElementById('guessForm').style.display = 'block';
        } else {
            document.getElementById('waitingMessage').style.display = 'block';
            document.getElementById('guessForm').style.display = 'none';
        }
    }
}

function updatePlayersList(players) {
    const playersList = document.getElementById('playersList');
    playersList.innerHTML = '';
    
    // Sort by score descending
    players.sort((a, b) => b.score - a.score);
    
    players.forEach(player => {
        const playerDiv = document.createElement('div');
        playerDiv.className = 'player-item';
        
        if (player.isLeader) {
            playerDiv.classList.add('is-leader');
        }
        
        if (player.id === currentPlayerId) {
            playerDiv.classList.add('is-you');
        }
        
        let badges = '';
        if (player.isLeader) {
            badges += '<span class="badge badge-leader">Ведущий</span>';
        }
        if (player.id === currentPlayerId) {
            badges += '<span class="badge badge-you">Вы</span>';
        }
        
        playerDiv.innerHTML = `
            <div>
                <span class="player-name">${player.name}${badges}</span>
            </div>
            <div>
                <span class="player-score">${player.score} 🏆</span>
            </div>
        `;
        
        playersList.appendChild(playerDiv);
    });
}

function updateWinnerButtons(players) {
    const playerButtons = document.getElementById('playerButtons');
    playerButtons.innerHTML = '';
    
    // Filter out the leader (current player)
    const otherPlayers = players.filter(p => !p.isLeader);
    
    if (otherPlayers.length === 0) {
        playerButtons.innerHTML = '<p style="color: #666;">Ожидание других игроков...</p>';
        return;
    }
    
    otherPlayers.forEach(player => {
        const button = document.createElement('button');
        button.className = 'btn btn-winner';
        button.textContent = player.name;
        button.onclick = () => assignWinner(player.id);
        playerButtons.appendChild(button);
    });
}

async function generateNewWord() {
    try {
        const response = await fetch(`/api/rooms/${ROOM_CODE}/new-word`, {
            method: 'POST'
        });
        
        if (!response.ok) {
            throw new Error('Failed to generate word');
        }
        
        const data = await response.json();
        document.getElementById('wordDisplay').textContent = data.word;
    } catch (error) {
        console.error('Error generating word:', error);
        alert('Не удалось сгенерировать слово');
    }
}

async function submitGuess(guess) {
    try {
        const response = await fetch(`/api/rooms/${ROOM_CODE}/guess`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ guess: guess })
        });
        
        if (!response.ok) {
            throw new Error('Failed to submit guess');
        }
        
        const data = await response.json();
        
        const feedback = document.getElementById('guess-feedback');
        feedback.textContent = data.message;
        feedback.className = 'feedback-message show';
        
        if (data.correct) {
            feedback.classList.add('success');
            document.getElementById('guess').value = '';
            
            // Reload state immediately
            await loadRoomState();
        } else {
            feedback.classList.add('error');
        }
        
        setTimeout(() => {
            feedback.classList.remove('show');
        }, 3000);
        
    } catch (error) {
        console.error('Error submitting guess:', error);
    }
}

async function assignWinner(winnerId) {
    try {
        const response = await fetch(`/api/rooms/${ROOM_CODE}/assign-winner`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ winnerId: winnerId })
        });
        
        if (!response.ok) {
            throw new Error('Failed to assign winner');
        }
        
        const data = await response.json();
        alert(data.message);
        
        // Reload state immediately
        await loadRoomState();
    } catch (error) {
        console.error('Error assigning winner:', error);
        alert('Не удалось назначить победителя');
    }
}

async function leaveRoom() {
    if (!confirm('Вы уверены, что хотите покинуть комнату?')) {
        return;
    }
    
    try {
        await fetch(`/api/rooms/${ROOM_CODE}/leave`, {
            method: 'POST'
        });
        
        stopPolling();
        window.location.href = '/';
    } catch (error) {
        console.error('Error leaving room:', error);
        window.location.href = '/';
    }
}

function startPolling() {
    // Initial load
    loadRoomState();
    
    // Poll every 2 seconds
    pollingInterval = setInterval(loadRoomState, 2000);
}

function stopPolling() {
    if (pollingInterval) {
        clearInterval(pollingInterval);
        pollingInterval = null;
    }
}

function showJoinError(message) {
    const errorDiv = document.getElementById('join-error');
    errorDiv.textContent = message;
    errorDiv.classList.add('show');
    
    setTimeout(() => {
        errorDiv.classList.remove('show');
    }, 5000);
}

// Stop polling when page is hidden
document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        stopPolling();
    } else if (currentPlayerId) {
        startPolling();
    }
});

