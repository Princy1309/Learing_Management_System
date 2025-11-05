document.addEventListener('DOMContentLoaded', () => {
    // Main page elements
    const tableBody = document.getElementById('coursesTableBody');
    const logoutBtn = document.getElementById('logoutBtn');
    const token = localStorage.getItem('token');

    // Modal elements
    const modal = document.getElementById('editCourseModal');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const saveChangesBtn = document.getElementById('saveChangesBtn');
    const addLessonModalBtn = document.getElementById('addLessonModalBtn');
    const modalStatus = document.getElementById('modalStatus');

    if (!token) {
        window.location.href = '/login';
        return;
    }
    const jsonHeaders = { 
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
    };
    const fileUploadHeaders = { 
        'Authorization': 'Bearer ' + token 
    };

    /**
     * Helper to generate a media preview (video, image, audio)
     */
    const generatePreviewHtml = (type, url) => {
        if (!url) return '<p class="text-xs text-gray-500">No file uploaded or URL provided.</p>';
        switch (type) {
            case 'video':
                return `<video controls src="${url}" class="w-full max-h-48 mt-2 rounded bg-gray-100"></video>`;
            case 'audio':
                return `<audio controls src="${url}" class="w-full mt-2"></audio>`;
            case 'image':
                return `<img src="${url}" class="w-full max-h-48 mt-2 rounded object-contain bg-gray-100"/>`;
            case 'pdf':
                return `<a href="${url}" target="_blank" class="text-indigo-600 hover:underline mt-2 block">View current PDF</a>`;
            default:
                return `<a href="${url}" target="_blank" class="text-indigo-600 hover:underline mt-2 block">View current file</a>`;
        }
    };

    /**
     * Helper to show/hide inputs based on lesson type
     */
    const updateLessonInputs = (wrapper, type) => {
        const fileInput = wrapper.querySelector('[data-lesson-file]');
        const fileInputsContainer = wrapper.querySelector('[data-file-inputs]');
        const textInputContainer = wrapper.querySelector('[data-text-input]');

        if (type === 'text') {
            fileInputsContainer.classList.add('hidden');
            textInputContainer.classList.remove('hidden');
            fileInput.accept = '';
        } else {
            fileInputsContainer.classList.remove('hidden');
            textInputContainer.classList.add('hidden');
            switch (type) {
                case 'video': fileInput.accept = 'video/*'; break;
                case 'image': fileInput.accept = 'image/*'; break;
                case 'pdf': fileInput.accept = '.pdf,application/pdf'; break;
                case 'audio': fileInput.accept = 'audio/*'; break;
                default: fileInput.accept = '*/*';
            }
        }
    };

    /**
     * NEW: Helper to validate the selected file type
     */
    const isFileTypeValid = (file, expectedType) => {
        if (!file || !expectedType) return false;
        if (expectedType === 'text') return true; // Not a file
        
        const fileMimeType = file.type; // e.g., "image/png"
        
        if (expectedType === 'pdf') {
            return fileMimeType === 'application/pdf';
        }
        
        // Checks "video/mp4" starts with "video"
        return fileMimeType.startsWith(expectedType);
    };


    /**
     * REBUILT: createModalLessonBlock with all inputs, previews, and validation
     */
    const createModalLessonBlock = (lesson = {}) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'p-3 border rounded';
        wrapper.innerHTML = `
            <div class="flex items-start gap-3">
                <input type="hidden" data-lesson-id value="${lesson.id || ''}">
                <div class="flex-1">
                    <input data-lesson-title class="w-full border p-2 rounded mb-2" placeholder="Lesson title" value="${lesson.title || ''}">
                    <select data-lesson-type class="w-full border p-2 rounded mb-2">
                        <option value="video" ${lesson.contentType === 'video' ? 'selected' : ''}>Video</option>
                        <option value="pdf" ${lesson.contentType === 'pdf' ? 'selected' : ''}>PDF</option>
                        <option value="image" ${lesson.contentType === 'image' ? 'selected' : ''}>Image</option>
                        <option value="audio" ${lesson.contentType === 'audio' ? 'selected' : ''}>Audio</option>
                        <option value="text" ${lesson.contentType === 'text' ? 'selected' : ''}>Text</option>
                    </select>
                    
                    <div data-file-inputs class="space-y-2">
                        <input data-lesson-file type="file" class="w-full text-sm">
                        <input data-lesson-url type="text" class="w-full border p-2 rounded" placeholder="(or paste file URL)" value="${(lesson.contentType !== 'text' && lesson.contentUrl) ? lesson.contentUrl : ''}">
                        <div data-preview-container class="mt-2 p-2 border rounded bg-gray-50 text-center">
                            </div>
                    </div>

                    <div data-text-input class="hidden">
                        <textarea data-lesson-text class="w-full border p-2 rounded" rows="3" placeholder="Enter lesson text content">${(lesson.contentType === 'text' && lesson.contentUrl) ? lesson.contentUrl : ''}</textarea>
                    </div>
                </div>
                <button data-remove class="text-red-600 px-2 py-1 self-center">Remove</button>
            </div>
        `;

        // --- Correctly select all elements ---
        const typeSelect = wrapper.querySelector('[data-lesson-type]');
        const fileInput = wrapper.querySelector('[data-lesson-file]');
        const urlInput = wrapper.querySelector('[data-lesson-url]');
        const previewContainer = wrapper.querySelector('[data-preview-container]');

        // --- Event Listeners for Validation and Previews ---

        // 1. On Type Change: Clear inputs and update visibility
        typeSelect.addEventListener('change', (e) => {
            const newType = e.target.value;
            fileInput.value = ''; // CRITICAL: Clear file input
            urlInput.value = '';  // CRITICAL: Clear URL input
            previewContainer.innerHTML = '<p class="text-xs text-gray-500">Select a file to preview.</p>'; // Clear preview
            updateLessonInputs(wrapper, newType);
        });

        // 2. On New File Select: Validate type, update preview, and clear URL input
        fileInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            const currentType = typeSelect.value;
            
            if (!file) {
                previewContainer.innerHTML = '<p class="text-xs text-gray-500">No file selected.</p>';
                return;
            }

            // --- VALIDATION FIX ---
            if (!isFileTypeValid(file, currentType)) {
                alert(`Invalid file type. You selected ${currentType}, but the file is a ${file.type}.`);
                fileInput.value = ''; // Clear the invalid file
                previewContainer.innerHTML = '<p class="text-xs text-red-500">Invalid file type. Please select a valid file.</p>';
                return;
            }
            // --- END VALIDATION FIX ---
            
            urlInput.value = ''; // File upload takes precedence, clear URL
            
            const localUrl = URL.createObjectURL(file);
            if (currentType === 'pdf') {
                previewContainer.innerHTML = `<p class="text-sm text-gray-700 font-medium">New file selected: ${file.name}</p>`;
            } else {
                previewContainer.innerHTML = generatePreviewHtml(currentType, localUrl);
            }
        });

        // --- Set Initial State ---
        updateLessonInputs(wrapper, typeSelect.value);
        previewContainer.innerHTML = generatePreviewHtml(lesson.contentType, lesson.contentUrl); // Show existing file preview

        wrapper.querySelector('[data-remove]').addEventListener('click', () => wrapper.remove());
        return wrapper;
    };
    
    // --- (Modal Controls: showModal, hideModal... remain the same) ---
    const showModal = () => modal.classList.remove('hidden');
    const hideModal = () => modal.classList.add('hidden');

    // --- (loadCourses function remains the same) ---
    const loadCourses = async () => {
        try {
            const response = await fetch('/api/instructor/my-courses', { headers: jsonHeaders });
            if (!response.ok) throw new Error('Failed to fetch courses');
            
            const courses = await response.json();
            
            tableBody.innerHTML = '';
            courses.forEach(course => {
                const row = document.createElement('tr');
                row.setAttribute('data-course-id', course.id);
                const statusBadge = course.approved
                    ? `<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">Approved</span>`
                    : `<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">Pending</span>`;

                row.innerHTML = `
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="text-sm font-medium text-gray-900">${course.title}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="text-sm font-medium text-gray-900">${course.description}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap status-cell">${statusBadge}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium action-cell">
                        <button data-action="edit" class="text-indigo-600 hover:text-indigo-900 mr-3">Edit</button>
                        <button data-action="delete" class="text-red-600 hover:text-red-900">Delete</button>
                    </td>
                `;
                tableBody.appendChild(row);
            });
        } catch (error) {
            tableBody.innerHTML = `<tr><td colspan="4" class="text-center p-4 text-red-500">${error.message}</td></tr>`;
        }
    };
    
    // --- (tableBody event listener remains the same, with the res.ok check) ---
    tableBody.addEventListener('click', async (e) => {
        const target = e.target;
        const action = target.getAttribute('data-action');
        if (!action) return;

        const row = target.closest('tr');
        const courseId = row.getAttribute('data-course-id');

        if (action === 'delete') {
            if (confirm('Are you sure you want to delete this course?')) {
                try {
                    const res = await fetch(`/api/instructor/courses/${courseId}`, { method: 'DELETE', headers: fileUploadHeaders });
                    if (!res.ok) throw new Error(await res.text() || 'Failed to delete.');
                    row.remove();
                } catch (err) { alert(err.message); }
            }
        }

        if (action === 'approve') {
            try {
                target.disabled = true;
                target.textContent = 'Approving...';
                const res = await fetch(`/api/instructor/courses/${courseId}/approve`, { method: 'POST', headers: fileUploadHeaders });
                if (!res.ok) throw new Error('Failed to approve.');
                const statusCell = row.querySelector('.status-cell');
                statusCell.innerHTML = `<span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">Approved</span>`;
            } catch (err) {
                alert(err.message);
                target.disabled = false;
                target.textContent = 'Approve';
            }
        }
        
        if (action === 'edit') {
            try {
                const res = await fetch(`/api/instructor/courses/${courseId}`, { headers: jsonHeaders });
                if (!res.ok) throw new Error(await res.text() || `Server error: ${res.status}`);
                
                const course = await res.json();
                
                document.getElementById('editCourseId').value = course.id;
                document.getElementById('editCourseTitle').value = course.title;
                document.getElementById('editCourseDescription').value = course.description;
                
                const lessonsContainer = document.getElementById('editLessonsContainer');
                lessonsContainer.innerHTML = '';
                course.lessons.sort((a, b) => a.lessonOrder - b.lessonOrder).forEach(lesson => {
                    lessonsContainer.appendChild(createModalLessonBlock(lesson));
                });
                
                modalStatus.textContent = '';
                showModal();
            } catch(err) { 
                alert('Could not load course data: ' + err.message); 
            }
        }
    });

    // --- (addLessonModalBtn listener remains the same) ---
    addLessonModalBtn.addEventListener('click', () => {
        document.getElementById('editLessonsContainer').appendChild(createModalLessonBlock());
    });
    
    // --- (closeModalBtn listener remains the same) ---
    closeModalBtn.addEventListener('click', hideModal);

    // --- REBUILT: saveChangesBtn listener with correct file upload logic ---
    saveChangesBtn.addEventListener('click', async () => {
        modalStatus.textContent = 'Saving... Please wait.';
        saveChangesBtn.disabled = true;

        const courseId = document.getElementById('editCourseId').value;
        const payload = {
            title: document.getElementById('editCourseTitle').value,
            description: document.getElementById('editCourseDescription').value,
            lessons: []
        };
        const lessonBlocks = document.querySelectorAll('#editLessonsContainer > div');

        try {
            // Use a for...of loop to handle async file uploads sequentially
            for (const [index, block] of lessonBlocks.entries()) {
                const lessonId = block.querySelector('[data-lesson-id]').value;
                const ltitle = block.querySelector('[data-lesson-title]').value.trim();
                const ltype = block.querySelector('[data-lesson-type]').value;
                let contentUrl = '';

                if (ltype === 'text') {
                    contentUrl = block.querySelector('[data-lesson-text]').value.trim();
                } else {
                    const lfileInput = block.querySelector('[data-lesson-file]');
                    const lurlInput = block.querySelector('[data-lesson-url]').value.trim();

                    contentUrl = lurlInput || null; // Use URL input as default

                    if (lfileInput.files.length > 0) {
                        modalStatus.textContent = `Uploading file for lesson ${index + 1}...`;
                        const fd = new FormData();
                        fd.append('file', lfileInput.files[0]);

                        const res = await fetch('/api/files/upload', {
                            method: 'POST',
                            body: fd,
                            headers: fileUploadHeaders
                        });
                        
                        if (!res.ok) throw new Error(`File upload failed: ${await res.text()}`);
                        
                        const text = await res.text();
                        const urlMatch = text.match(/https?:\/\/\S+/);
                        if (urlMatch) {
                            contentUrl = urlMatch[0];
                        } else {
                            contentUrl = text.trim();
                        }
                    }
                }

                payload.lessons.push({
                    id: lessonId ? parseInt(lessonId, 10) : null,
                    title: ltitle || ('Lesson ' + (index + 1)),
                    contentType: ltype,
                    contentUrl: contentUrl,
                    lessonOrder: index + 1
                });
            } // End of loop

            modalStatus.textContent = 'Finalizing course update...';
            const res = await fetch(`/api/instructor/courses/${courseId}`, {
                method: 'PUT',
                headers: jsonHeaders,
                body: JSON.stringify(payload)
            });

            if (!res.ok) throw new Error(await res.text());
            
            modalStatus.textContent = 'Saved successfully!';
            setTimeout(() => {
                hideModal();
                loadCourses(); // Refresh the table
            }, 1000);

        } catch(err) {
            modalStatus.textContent = `Error: ${err.message}`;
        } finally {
            saveChangesBtn.disabled = false; // Re-enable the save button
        }
    });

    // --- Initial Load ---
    loadCourses();
});
