document.addEventListener('DOMContentLoaded', () => {
    // Page elements
    const courseSelector = document.getElementById('courseSelector');
    const tableBody = document.getElementById('studentsTableBody');
    const logoutBtn = document.getElementById('logoutBtn');
    const token = localStorage.getItem('token');

    if (!token) {
        window.location.href = '/login';
        return;
    }
    const headers = { 'Authorization': 'Bearer ' + token };

    /**
     * Fetches the instructor's courses and populates the dropdown.
     */
    const populateCourseDropdown = async () => {
        try {
            const response = await fetch('/api/instructor/my-courses', { headers });
            if (!response.ok) throw new Error('Could not fetch your courses.');
            
            const courses = await response.json();
            courses.forEach(course => {
                const option = document.createElement('option');
                option.value = course.id;
                option.textContent = course.title;
                courseSelector.appendChild(option);
            });
        } catch (error) {
            console.error(error);
            // Optionally show an error message to the user
        }
    };

    /**
     * Fetches and renders the students for the selected course.
     */
    const loadEnrolledStudents = async (courseId) => {
        tableBody.innerHTML = `<tr><td colspan="3" class="text-center p-4 text-gray-500">Loading students...</td></tr>`;

        try {
            const response = await fetch(`/api/instructor/courses/${courseId}/enrolled-students`, { headers });
            if (!response.ok) {
                 const errData = await response.json();
                 throw new Error(errData.error || 'Failed to load students.');
            }

            const students = await response.json();
            
            if (students.length === 0) {
                tableBody.innerHTML = `<tr><td colspan="3" class="text-center p-4 text-gray-500">No students are enrolled in this course yet.</td></tr>`;
                return;
            }

            tableBody.innerHTML = ''; // Clear loading message
            students.forEach(student => {
                const percentage = student.totalLessons > 0 
                    ? Math.round((student.completedLessons / student.totalLessons) * 100)
                    : 0;

                const row = document.createElement('tr');
                row.innerHTML = `
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="text-sm font-medium text-gray-900">${student.studentName}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="text-sm text-gray-600">${student.studentEmail}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="flex items-center">
                            <div class="w-full bg-gray-200 rounded-full h-2.5 mr-2">
                                <div class="bg-indigo-600 h-2.5 rounded-full" style="width: ${percentage}%"></div>
                            </div>
                            <span class="text-sm font-medium text-gray-700">${percentage}%</span>
                        </div>
                         <span class="text-xs text-gray-500">${student.completedLessons} / ${student.totalLessons} lessons</span>
                    </td>
                `;
                tableBody.appendChild(row);
            });

        } catch (error) {
            tableBody.innerHTML = `<tr><td colspan="3" class="text-center p-4 text-red-500">${error.message}</td></tr>`;
        }
    };

    // --- Event Listeners ---
    courseSelector.addEventListener('change', () => {
        const selectedCourseId = courseSelector.value;
        if (selectedCourseId) {
            loadEnrolledStudents(selectedCourseId);
        } else {
            tableBody.innerHTML = `<tr><td colspan="3" class="text-center p-4 text-gray-500">Select a course to see enrolled students.</td></tr>`;
        }
    });
    
    logoutBtn.addEventListener('click', () => {
        localStorage.clear();
        window.location.href = '/login';
    });

    // --- Initial Load ---
    populateCourseDropdown();
});