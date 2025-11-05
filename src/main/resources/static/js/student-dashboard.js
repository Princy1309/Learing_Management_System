document.addEventListener('DOMContentLoaded', () => {
  const token = localStorage.getItem('token');
  const logoutBtn = document.getElementById('logoutBtn');
  logoutBtn.addEventListener('click', () => {
    localStorage.clear();
    window.location.href = '/login';
  });

  if (!token) {
    loadCoursesPublic();
    return;
  }
loadMyCourses();
  loadCourses();
  loadEnrollments();
});

async function loadCoursesPublic() {
  console.log('[Student JS] loading public courses (no token)');
  const res = await fetch('/api/student/courses');
  console.log('[Student JS] loadCoursesPublic status', res.status);
  const container = document.getElementById('coursesGrid');
  container.innerHTML = '';
  if (res.ok) {
    const courses = await res.json();
    renderCourses(courses);
  } else {
    container.textContent = 'Failed to load courses: ' + res.status;
  }
}

async function loadCourses() {
  console.log('[Student JS] loading courses with token');
  const res = await fetch('/api/student/courses', {
    headers: { 'Authorization': 'Bearer ' + localStorage.getItem('token') }
  });
  console.log('[Student JS] loadCourses status', res.status);
  const container = document.getElementById('coursesGrid');
  container.innerHTML = '';

  if (res.ok) {
    const courses = await res.json();
    renderCourses(courses);
  } else {
    container.textContent = 'Failed to load courses: ' + res.status;
  }
}

function renderCourses(courses) {
  const container = document.getElementById('coursesGrid');
  container.innerHTML = '';
  courses.forEach(c => {
    const card = document.createElement('div');
    card.className = 'bg-white p-5 rounded-lg shadow hover:shadow-md transition';
    card.innerHTML = `
      <h3 class="font-semibold text-lg text-[#1c2541] mb-2">${escapeHtml(c.title)}</h3>
      <p class="text-sm text-gray-600 mb-4">${escapeHtml(c.description || '')}</p>
      <div class="flex gap-2">
        <button class="enrollBtn bg-[#5bc0be] hover:bg-[#4fa3a1] text-white px-4 py-1 rounded" data-id="${c.id}">Enroll</button>
      </div>
    `;
    container.appendChild(card);
  });

  container.querySelectorAll('.enrollBtn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.dataset.id;
      console.log('[Student JS] enrolling course id', id);

      const token = localStorage.getItem('token');
      const role = (localStorage.getItem('role') || '').toUpperCase();
      console.log('[Student JS] token:', token);
      console.log('[Student JS] role:', role);

      if (!token) {
        alert('Please login to enroll');
        return;
      }

      if (role !== 'STUDENT') {
        alert('Only STUDENT users can enroll. Your role: ' + role);
        return;
      }

      const res = await fetch(`/api/student/enroll/${id}`, {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + token }
      });

      console.log('[Student JS] enroll status', res.status);
      if (res.ok) {
        alert('Enrolled successfully');
        loadEnrollments();
      } else {
        const txt = await res.text();
        console.error('[Student JS] enroll failed body:', txt);
        alert('Enroll failed: ' + res.status + ' - ' + txt);
      }
    });
  });
}

async function loadEnrollments() {
  const token = localStorage.getItem('token');
  if (!token) {
    document.getElementById('myCoursesGrid').textContent = 'Login to see your courses';
    return;
  }

  console.log('[Student JS] loading enrolled courses...');
  const res = await fetch('/api/student/my-courses', {
    headers: { 'Authorization': 'Bearer ' + token }
  });

  const container = document.getElementById('myCoursesGrid');
  container.innerHTML = '';

  if (res.ok) {
    const courses = await res.json();
    renderMyCourses(courses);
  } else {
    container.textContent = 'Failed to load enrolled courses: ' + res.status;
  }
}

function renderMyCourses(courses) {
  const container = document.getElementById('myCoursesGrid');
  container.innerHTML = '';

  if (courses.length === 0) {
    container.textContent = 'You have not enrolled in any courses yet.';
    return;
  }

  courses.forEach(c => {
    const card = document.createElement('div');
    card.className = 'bg-white p-5 rounded-lg shadow hover:shadow-md transition';
    card.innerHTML = `
      <h3 class="font-semibold text-lg text-[#1c2541] mb-2">${escapeHtml(c.title)}</h3>
      <p class="text-sm text-gray-600 mb-4">${escapeHtml(c.description || '')}</p>
      <a href="/student/course/${c.id}" class="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-1 rounded">View</a>
    `;
    container.appendChild(card);
  });
}

async function loadMyCourses() {
    const token = localStorage.getItem('token');
    const container = document.getElementById('myCoursesGrid');

    // Check for token and container existence
    if (!token) {
        window.location.href = '/login'; // Redirect if not logged in
        return;
    }
    if (!container) {
        console.error('Error: The element with id "myCoursesGrid" was not found.');
        return;
    }
    
    try {
        const headers = { 'Authorization': 'Bearer ' + token };
        const response = await fetch('/api/student/my-courses', { headers });

        if (!response.ok) {
            throw new Error('Failed to fetch your courses from the server.');
        }

        const courses = await response.json();
        // Pass the fetched data to the rendering function
        renderMyCourses(courses);

    } catch (error) {
        container.innerHTML = `<p class="text-red-500 col-span-full">${error.message}</p>`;
        console.error('Error loading courses:', error);
    }
}

/**
 * Renders the course cards with progress bars into the grid.
 * This function ONLY displays data; it does not fetch it.
 * @param {Array} courses The array of course objects from the API.
 */
function renderMyCourses(courses) {
    const container = document.getElementById('myCoursesGrid');

    if (!courses || courses.length === 0) {
        container.innerHTML = '<p class="text-gray-500 col-span-full">You are not enrolled in any courses yet.</p>';
        return;
    }

    container.innerHTML = ''; // Clear any previous content

    courses.forEach(course => {
        const percentage = course.totalLessons > 0 
            ? Math.round((course.completedLessons / course.totalLessons) * 100)
            : 0;

        const courseCard = document.createElement('div');
        courseCard.className = 'bg-white rounded-lg shadow-md overflow-hidden flex flex-col transition-transform transform hover:-translate-y-1';
        
        courseCard.innerHTML = `
            <div class="p-5 flex-grow">
                <h3 class="text-lg font-semibold text-[#3a506b] mb-2">${escapeHtml(course.title)}</h3>
                <p class="text-gray-600 text-sm mb-4 line-clamp-2">${escapeHtml(course.description) || 'No description available.'}</p>
            </div>
            <div class="px-5 pb-5 mt-auto">
                <div class="mb-2">
                    <div class="flex justify-between text-xs text-gray-500 mb-1">
                        <span>Progress</span>
                        <span>${course.completedLessons} / ${course.totalLessons}</span>
                    </div>
                    <div class="w-full bg-gray-200 rounded-full h-2">
                        <div class="bg-indigo-600 h-2 rounded-full" style="width: ${percentage}%"></div>
                    </div>
                </div>
                <a href="/student/course/${course.id}" class="block w-full text-center bg-[#1c2541] text-white py-2 rounded-md hover:bg-[#3a506b] transition-colors">
                    View Course
                </a>
            </div>
        `;
        container.appendChild(courseCard);
    });
}
// tiny helper to avoid accidental html injection when injecting strings
function escapeHtml(s) {
  if (!s) return '';
  return s.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}
