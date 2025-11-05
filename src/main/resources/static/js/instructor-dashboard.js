function makeLessonBlock(index) {
    const wrapper = document.createElement('div');
    wrapper.className = 'p-3 border rounded flex items-start gap-3';

    wrapper.innerHTML = `
    <div class="flex-1">
      <input data-lesson-title class="w-full border p-2 rounded mb-2" placeholder="Lesson title">
      <select data-lesson-type class="w-full border p-2 rounded mb-2">
        <option value="video">Video</option>
        <option value="pdf">PDF</option>
        <option value="image">Image</option>
        <option value="audio">Audio</option>
        <option value="text">Text</option>
      </select>
      <input data-lesson-file type="file" class="mb-2" />
      <input data-lesson-url type="text" class="w-full border p-2 rounded" placeholder="(or paste file URL after upload)" />
    </div>
    <div class="flex flex-col gap-2">
      <button data-remove class="text-red-600 px-2 py-1">Remove</button>
    </div>
  `;

    // --- START: ADDED LOGIC FOR FILE TYPE FILTERING ---
    const typeSelect = wrapper.querySelector('[data-lesson-type]');
    const fileInput = wrapper.querySelector('[data-lesson-file]');

    // Helper function to update the 'accept' attribute
    const updateFileInput = (type) => {
        switch (type) {
            case 'video':
                fileInput.accept = 'video/*';
                break;
            case 'image':
                fileInput.accept = 'image/*';
                break;
            case 'pdf':
                fileInput.accept = '.pdf';
                break;
            case 'audio':
                fileInput.accept = 'audio/*';
                break;
            case 'text':
                fileInput.accept = ''; // No file needed, but reset
                break;
            default:
                fileInput.accept = '*/*';
        }
    };

    // 1. Add event listener to update on change
    typeSelect.addEventListener('change', (e) => {
        updateFileInput(e.target.value);
    });

    // 2. Set the initial value (which is "video")
    updateFileInput(typeSelect.value);

    wrapper.querySelector('[data-remove]').addEventListener('click', () => wrapper.remove());
    return wrapper;
}


document.addEventListener('DOMContentLoaded', () => {
  const lessonsContainer = document.getElementById('lessonsContainer');
  const addLessonBtn = document.getElementById('addLessonBtn');
  const createCourseBtn = document.getElementById('createCourseBtn');
  const clearBtn = document.getElementById('clearBtn');
  const status = document.getElementById('status');
  const logoutBtn = document.getElementById('logoutBtn');

  addLessonBtn.addEventListener('click', (e) => {
    e.preventDefault();
    lessonsContainer.appendChild(makeLessonBlock());
  });

  clearBtn.addEventListener('click', (e) => {
    e.preventDefault();
    document.getElementById('courseTitle').value = '';
    document.getElementById('courseDescription').value = '';
    lessonsContainer.innerHTML = '';
    status.textContent = '';
  });

  logoutBtn.addEventListener('click', (e) => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    window.location.href = '/';
  });

  createCourseBtn.addEventListener('click', async (e) => {
    e.preventDefault();
    status.textContent = 'Uploading files and creating course...';

    const title = document.getElementById('courseTitle').value.trim();
    const description = document.getElementById('courseDescription').value.trim();
    if (!title) { status.textContent = 'Title is required'; return; }

    const lessonBlocks = Array.from(lessonsContainer.children);
    const lessons = [];
    const token = localStorage.getItem('token');

    if (!token) {
      status.textContent = 'You must be logged in as an instructor.';
      return;
    }

    for (let i = 0; i < lessonBlocks.length; i++) {
      const block = lessonBlocks[i];
      const ltitle = block.querySelector('[data-lesson-title]').value.trim();
      const ltype = block.querySelector('[data-lesson-type]').value;
      const lfileInput = block.querySelector('[data-lesson-file]');
      const lurlInput = block.querySelector('[data-lesson-url]').value.trim();

      let contentUrl = lurlInput || null;

      if (lfileInput && lfileInput.files && lfileInput.files.length > 0) {
        const fd = new FormData();
        fd.append('file', lfileInput.files[0]);

        const res = await fetch('/api/files/upload', {
          method: 'POST',
          body: fd,
          headers: {
            'Authorization': 'Bearer ' + token
          }
        });

        const text = await res.text();
        const urlMatch = text.match(/https?:\/\/\S+/);
        if (urlMatch) contentUrl = urlMatch[0];
      }

      lessons.push({
        title: ltitle || ('Lesson ' + (i+1)),
        contentType: ltype,
        contentUrl: contentUrl,
        lessonOrder: i + 1
      });
    }

    const payload = { title, description, lessons };

    const res = await fetch('/api/instructor/courses', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify(payload)
    });

    if (res.ok) {
      const json = await res.json();
      status.textContent = 'Course created ✅ (id: ' + json.id + ')';
      document.getElementById('courseTitle').value = '';
      document.getElementById('courseDescription').value = '';
      lessonsContainer.innerHTML = '';
    } else {
      const text = await res.text();
      status.textContent = 'Failed: ' + text;
    }
  });

  // ensure role is instructor
  const role = (localStorage.getItem('role') || '').toUpperCase();
  if (role !== 'INSTRUCTOR') {
    status.textContent = 'You are not authorized. Please login as INSTRUCTOR.';
  }
});
