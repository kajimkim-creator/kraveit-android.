(async function () {
  'use strict';

  if (window.__kraveitNativeProfileInit) return;
  if (location.hostname !== 'kraveit.netlify.app') return;
  if (!/\/checkout\.html$/.test(location.pathname)) return;
  if (window !== window.top || !window.KraveitProfileMessages) return;
  var pending = new Map();
  var nextId = 0;
  window.KraveitProfileMessages.onmessage = function (event) {
    try {
      var reply = JSON.parse(event.data);
      var task = pending.get(reply.id);
      if (!task) return;
      pending.delete(reply.id);
      clearTimeout(task.timer);
      if (reply.ok) task.resolve(reply);
      else task.reject(new Error('Profile request failed'));
    } catch (e) {}
  };
  function request(action, profile) {
    return new Promise(function (resolve, reject) {
      var id = ++nextId;
      var timer = setTimeout(function () {
        pending.delete(id);
        reject(new Error('Profile request timed out'));
      }, 3000);
      pending.set(id, { resolve: resolve, reject: reject, timer: timer });
      try {
        window.KraveitProfileMessages.postMessage(JSON.stringify({ id: id, action: action, profile: profile }));
      } catch (e) {
        clearTimeout(timer);
        pending.delete(id);
        reject(e);
      }
    });
  }

  var nameField = document.getElementById('name');
  var phoneField = document.getElementById('phone');
  var distanceField = document.getElementById('distance');
  var notesField = document.getElementById('notes');
  var submitButton = document.getElementById('submitBtn');

  if (!nameField || !phoneField || !distanceField || !notesField || !submitButton) return;
  window.__kraveitNativeProfileInit = true;

  var profile = { saved: false };
  try {
    profile = (await request('get')).profile || { saved: false };
  } catch (e) {
    profile = { saved: false };
  }

  if (profile.saved) {
    if (!nameField.value && profile.name) nameField.value = profile.name;
    if (!phoneField.value && profile.phone) phoneField.value = profile.phone;
    if (['0', '50', '100', '150', 'quote'].indexOf(String(profile.distance)) !== -1) {
      distanceField.value = String(profile.distance);
      if (typeof window.renderSummary === 'function') window.renderSummary();
    }
    if (!notesField.value && profile.notes) notesField.value = profile.notes;
  }

  var box = document.createElement('div');
  box.className = 'compactInfo';
  box.setAttribute('data-kraveit-native-profile', 'true');

  var label = document.createElement('label');
  label.style.display = 'flex';
  label.style.gap = '8px';
  label.style.alignItems = 'center';
  label.style.cursor = 'pointer';

  var checkbox = document.createElement('input');
  checkbox.type = 'checkbox';
  checkbox.checked = !!profile.saved;
  checkbox.setAttribute('aria-label', 'Remember my details on this phone');

  var labelText = document.createElement('span');
  labelText.innerHTML = '<b>Remember my details on this phone</b>';

  label.appendChild(checkbox);
  label.appendChild(labelText);
  box.appendChild(label);

  var note = document.createElement('div');
  note.className = 'status';
  note.textContent = profile.saved
    ? 'Saved details loaded. You can edit them before ordering.'
    : 'Saves your name, phone and delivery preferences only. Payment codes are never saved.';
  box.appendChild(note);

  if (profile.saved) {
    var forgetButton = document.createElement('button');
    forgetButton.type = 'button';
    forgetButton.className = 'btn';
    forgetButton.textContent = 'Forget saved details';
    forgetButton.addEventListener('click', async function () {
      try { await request('clear'); } catch (e) {
        note.textContent = 'Could not remove saved details. Please try again.';
        return;
      }
      checkbox.checked = false;
      note.textContent = 'Saved details removed from this phone.';
      forgetButton.disabled = true;
    });
    box.appendChild(forgetButton);
  }

  submitButton.parentNode.insertBefore(box, submitButton);

  submitButton.addEventListener('click', async function () {
    if (!checkbox.checked) return;
    var payload = {
      name: String(nameField.value || '').trim(),
      phone: String(phoneField.value || '').trim(),
      distance: String(distanceField.value || '0'),
      notes: String(notesField.value || '').trim()
    };
    try {
      await request('save', payload);
      note.textContent = 'Details saved on this phone for your next order.';
    } catch (e) {}
  }, true);
})();
