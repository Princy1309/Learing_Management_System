document.addEventListener('DOMContentLoaded', () => {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login';
        return;
    }
    const headers = { 
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
    };

    // --- Page Elements ---
    const usersTableBody = document.getElementById('usersTableBody');
    const coursesTableBody = document.getElementById('coursesTableBody');
    const logoutBtn = document.getElementById('logoutBtn');

    // --- Modal Elements ---
    const modal = document.getElementById('addUserModal');
    const showAddUserModalBtn = document.getElementById('showAddUserModalBtn');
    const cancelAddUserBtn = document.getElementById('cancelAddUserBtn');
    const addUserForm = document.getElementById('addUserForm');
    const modalStatus = document.getElementById('modalStatus');

    // --- Show/Hide Modal ---
    showAddUserModalBtn.addEventListener('click', () => modal.classList.remove('hidden'));
    cancelAddUserBtn.addEventListener('click', () => modal.classList.add('hidden'));

    // --- Logout ---
    logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        window.location.href = '/';
    });

    /**
     * 1. LOAD USERS
     */
    async function loadUsers() {
        try {
            const res = await fetch('/api/admin/users', { headers });
            if (!res.ok) throw new Error('Failed to load users');
            
            const users = await res.json();
            usersTableBody.innerHTML = ''; // Clear table
            users.forEach(user => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="text-sm font-medium text-gray-900">${user.username}</div>
                        <div class="text-sm text-gray-500">${user.email}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                        <select data-user-id="${user.id}" class="role-select border border-slate-300 rounded-lg p-2">
                            <option value="STUDENT" ${user.role === 'STUDENT' ? 'selected' : ''}>Student</option>
                            <option value="INSTRUCTOR" ${user.role === 'INSTRUCTOR' ? 'selected' : ''}>Instructor</option>
                            <option value="ADMIN" ${user.role === 'ADMIN' ? 'selected' : ''}>Admin</option>
                        </select>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button data-action="save-role" data-user-id="${user.id}" class="text-indigo-600 hover:text-indigo-900 mr-4">Save</button>
                        <button data-action="delete-user" data-user-id="${user.id}" class="text-red-600 hover:text-red-900">Delete</button>
                    </td>
                `;
                usersTableBody.appendChild(row);
            });
        } catch (error) {
            usersTableBody.innerHTML = `<tr><td colspan="3" class="text-center p-4 text-red-500">${error.message}</td></tr>`;
        }
    }

    /**
     * 2. LOAD PENDING COURSES
     */
    async function loadPendingCourses() {
        try {
            const res = await fetch('/api/admin/courses/pending', { headers });
            if (!res.ok) throw new Error('Failed to load courses');
            
            const courses = await res.json();
            coursesTableBody.innerHTML = ''; // Clear table
            if (courses.length === 0) {
                 coursesTableBody.innerHTML = `<tr><td colspan="4" class="text-center p-4 text-gray-500">No courses pending approval.</td></tr>`;
                 return;
            }
            courses.forEach(course => {
                const row = document.createElement('tr');
                row.innerHTML = `
                    <td class="px-6 py-4 whitespace-nowrap">
                        <div class="text-sm font-medium text-gray-900">${course.title}</div>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        ${course.instructor ? course.instructor.username : 'N/A'}
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                        <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-yellow-100 text-yellow-800">Pending</span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button data-action="approve" data-course-id="${course.id}" class="text-green-600 hover:text-green-900 mr-4">Approve</button>
                        <button data-action="remove" data-course-id="${course.id}" class="text-red-600 hover:text-red-900">Remove</button>
                    </td>
                `;
                coursesTableBody.appendChild(row);
            });
        } catch (error) {
            coursesTableBody.innerHTML = `<tr><td colspan="4" class="text-center p-4 text-red-500">${error.message}</td></tr>`;
        }
    }

    /**
     * 3. HANDLE ADD USER FORM SUBMISSION
     */
    addUserForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(addUserForm);
        const data = Object.fromEntries(formData.entries());
        
        modalStatus.textContent = '';
        try {
            const res = await fetch('/api/admin/register', {
                method: 'POST',
                headers,
                body: JSON.stringify(data)
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.error || 'Failed to create user.');
            }
            alert('User created successfully!');
            modal.classList.add('hidden'); // Hide modal
            addUserForm.reset(); // Clear the form
            loadUsers(); // Refresh the user list
        } catch (error) {
            modalStatus.textContent = error.message;
        }
    });

    /**
     * 4. EVENT DELEGATION FOR TABLE ACTIONS
     */
    document.body.addEventListener('click', async (e) => {
        const action = e.target.dataset.action;
        if (!action) return; // Exit if the click wasn't on an action button

        // --- User Actions ---
        if (action === 'save-role') {
            const userId = e.target.dataset.userId;
            const role = document.querySelector(`select[data-user-id="${userId}"]`).value;
            e.target.textContent = "Saving...";
            try {
                const res = await fetch(`/api/admin/users/${userId}/role`, {
                    method: 'PUT', headers, body: JSON.stringify({ role })
                });
                if (!res.ok) throw new Error('Save failed');
                e.target.textContent = "Saved!";
                setTimeout(() => e.target.textContent = "Save", 1500);
            } catch (err) { alert(err.message); e.target.textContent = "Save"; }
        }

        if (action === 'delete-user') {
            const userId = e.target.dataset.userId;
            if (confirm('Are you sure you want to delete this user? This cannot be undone.')) {
                try {
                    const res = await fetch(`/api/admin/users/${userId}`, { method: 'DELETE', headers });
                    if (!res.ok) throw new Error('Delete failed');
                    e.target.closest('tr').remove(); // Remove row from table
                } catch (err) { alert(err.message); }
            }
        }

        // --- Course Actions ---
        if (action === 'approve') {
            const courseId = e.target.dataset.courseId;
            e.target.disabled = true;
            try {
                const res = await fetch(`/api/admin/courses/${courseId}/approve`, { method: 'PUT', headers });
                if (!res.ok) throw new Error('Approve failed');
                e.target.closest('tr').remove(); // Remove from pending list
            } catch (err) { alert(err.message); e.target.disabled = false; }
        }

        if (action === 'remove') {
            const courseId = e.target.dataset.courseId;
            if (confirm('Are you sure you want to remove this course?')) {
                try {
                    const res = await fetch(`/api/admin/courses/${courseId}`, { method: 'DELETE', headers });
                    if (!res.ok) throw new Error('Remove failed');
                    e.target.closest('tr').remove(); // Remove from pending list
                } catch (err) { alert(err.message); }
            }
        }
    });

    // --- Initial Page Load ---
    loadUsers();
    loadPendingCourses();
});