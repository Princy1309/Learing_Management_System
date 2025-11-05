document.addEventListener('DOMContentLoaded', async () => {
    // UI Elements
    const statusDiv = document.getElementById('status');
    const lessonsDiv = document.getElementById('lessons');
    const titleEl = document.getElementById('title');
    const descEl = document.getElementById('desc');
    const progressContainer = document.getElementById('progress-container');
    const progressBar = document.getElementById('progress-bar');
    const progressText = document.getElementById('progress-text');

    // --- 1. DATA FETCHING & HEADERS ---
    const courseId = location.pathname.split('/').pop();
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login'; // Redirect if not logged in
        return;
    }
    const headers = { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' };
    
    // --- 2. DYNAMICALLY UNLOCK NEXT LESSON ---
    const unlockNextLesson = (completedCard) => {
        const nextLessonCard = completedCard.nextElementSibling;
        if (nextLessonCard && nextLessonCard.classList.contains('locked')) {
            nextLessonCard.classList.remove('locked', 'opacity-50', 'bg-gray-50');
            
            // Find the action area and replace the lock with a button
            const actionArea = nextLessonCard.querySelector('.lesson-action');
            if (actionArea) {
                actionArea.innerHTML = `
                    <button class="manual-complete-btn bg-indigo-500 text-white px-4 py-2 rounded-md hover:bg-indigo-600 text-sm">
                        <i class="fas fa-check mr-2"></i>Mark as Complete
                    </button>`;
            }
            // Enable any media content
            const media = nextLessonCard.querySelector('video, audio');
            if(media) media.disabled = false;
        }
    };

    // --- 3. CORE API CALL FUNCTION ---
    const markLessonComplete = async (lessonId, lessonCardElement) => {
        // Prevent re-completing an already completed lesson
        if (lessonCardElement.classList.contains('completed')) return;
        
        // Show immediate feedback
        const actionArea = lessonCardElement.querySelector('.lesson-action');
        const button = actionArea.querySelector('button');
        if (button) {
            button.disabled = true;
            button.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Completing...';
        }

        try {
            const res = await fetch(`/api/student/lessons/${lessonId}/complete`, {
                method: 'POST',
                headers: headers
            });

            if (!res.ok) throw new Error('Server responded with an error.');

            // Update UI on success
            lessonCardElement.classList.add('completed');
            lessonCardElement.classList.remove('locked');
            actionArea.innerHTML = `<div class="flex items-center text-green-600 font-bold"><i class="fas fa-check-circle mr-2"></i>Completed</div>`;
            
            // Unlock the next lesson visually
            unlockNextLesson(lessonCardElement);
            
            // Update the main progress bar
            updateOverallProgress();

        } catch (err) {
            console.error('Failed to mark lesson as complete:', err);
            alert('An error occurred while saving your progress.');
            // Revert button text on failure
            if(button) {
                button.disabled = false;
                button.innerHTML = '<i class="fas fa-check mr-2"></i>Mark as Complete';
            }
        }
    };
    
    // --- 4. UI RENDERING LOGIC ---
    const renderLessons = (lessons) => {
        if (!lessons || lessons.length === 0) {
            lessonsDiv.innerHTML = '<p class="text-gray-500">No lessons are available for this course yet.</p>';
            return;
        }

        lessonsDiv.innerHTML = ''; // Clear previous content
        lessons.forEach(lesson => {
            const card = document.createElement('div');
            card.className = 'lesson-card bg-white p-4 rounded-lg shadow-sm border-l-4 border-transparent transition-all duration-300';
            card.dataset.lessonId = lesson.id;

            let actionHtml = '';
            let cardClasses = '';
            let mediaDisabled = false;

            if (lesson.completed) {
                actionHtml = `<div class="flex items-center text-green-600 font-bold"><i class="fas fa-check-circle mr-2"></i>Completed</div>`;
                cardClasses = 'completed'; // For styling completed lessons
            } else if (lesson.accessible) {
                actionHtml = `<button class="manual-complete-btn bg-indigo-500 text-white px-4 py-2 rounded-md hover:bg-indigo-600 text-sm">
                                <i class="fas fa-check mr-2"></i>Mark as Complete
                              </button>`;
            } else {
                actionHtml = `<div class="flex items-center text-gray-500"><i class="fas fa-lock mr-2"></i>Locked</div>`;
                cardClasses = 'locked opacity-50 bg-gray-50'; // For styling locked lessons
                mediaDisabled = true;
            }
            if(cardClasses) card.classList.add(...cardClasses.split(' '));

            // ... (your existing contentHtml logic for video, audio, pdf, etc.)
            let contentHtml = '...'; // Keep your switch statement here
            if (lesson.contentUrl) {
                switch (lesson.contentType) {
                    case 'video':
                        contentHtml = `<video data-lesson-id="${lesson.id}" controls src="${lesson.contentUrl}" class="w-full mt-3 rounded-md"></video>`;
                        break;
                    case 'audio':
                        contentHtml = `<audio data-lesson-id="${lesson.id}" controls src="${lesson.contentUrl}" class="w-full mt-3"></audio>`;
                        break;
                    case 'pdf':
                        contentHtml = `<a href="${lesson.contentUrl}" target="_blank" class="block mt-3 text-indigo-600 hover:underline"><i class="fas fa-file-pdf mr-2"></i>View PDF Document</a>`;
                        break;
                    case 'image':
                         contentHtml = `<img src="${lesson.contentUrl}" class="w-full mt-3 rounded-md max-h-96 object-contain"/>`;
                         break;
                    default:
                        contentHtml = `<a href="${lesson.contentUrl}" target="_blank" class="block mt-3 text-indigo-600 hover:underline"><i class="fas fa-link mr-2"></i>Open Content</a>`;
                }
            } else {
                contentHtml = '<p class="text-gray-500 mt-2">No downloadable content for this lesson.</p>';
            }


            card.innerHTML = `
                <div class="flex justify-between items-start">
                    <h3 class="font-bold text-lg text-gray-800">${lesson.title}</h3>
                    <div class="lesson-action">${actionHtml}</div>
                </div>
                ${contentHtml}
            `;
            lessonsDiv.appendChild(card);
        });

        updateOverallProgress();
    };
const updateOverallProgress = () => {
        const allLessons = document.querySelectorAll('.lesson-card');
        const completedLessons = document.querySelectorAll('.lesson-card.completed');
        
        if (allLessons.length === 0) {
            progressContainer.classList.add('hidden');
            return;
        }

        progressContainer.classList.remove('hidden');
        const percentage = Math.round((completedLessons.length / allLessons.length) * 100);
        progressBar.style.width = `${percentage}%`;
        progressText.textContent = `${percentage}% Complete (${completedLessons.length} / ${allLessons.length})`;
    };
    
    
    // --- 5. EVENT DELEGATION FOR CLICK HANDLING ---
    lessonsDiv.addEventListener('click', (e) => {
        const button = e.target.closest('.manual-complete-btn');
        if (button) {
            const lessonCard = e.target.closest('.lesson-card');
            if (lessonCard) {
                markLessonComplete(lessonCard.dataset.lessonId, lessonCard);
            }
        }
    });
    try {
        const res = await fetch(`/api/student/courses/${courseId}`, { headers });
        if (!res.ok) throw new Error(await res.text());

        const course = await res.json();
        titleEl.textContent = course.title;
        descEl.textContent = course.description;

        // Remember to modify your CourseDto to include the 'completed' flag in each LessonDto
        renderLessons(course.lessons); 

    } catch (err) {
        statusDiv.textContent = `Failed to load course: ${err.message}`;
        console.error(err);
    }
  
});