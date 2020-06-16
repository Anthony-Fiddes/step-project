// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

function onPageLoad() {
  useBlobForm();
  getComments();
}

async function useBlobForm() {
  const blobResponse = await fetch('/blobstore-upload-url');
  const blobURL = await blobResponse.text();
  const form = document.getElementById('comments-form');
  form.setAttribute('action', blobURL);
  form.setAttribute('enctype', 'multipart/form-data');
  form.classList.remove('hidden');
}

/**
 * Adds comments from /data
 */
async function getComments() {
  const max = document.getElementById('max').value;
  const language = document.getElementById('language').value;
  const commentsData = await fetch('/data?max=' + max + '&language=' + language);
  const comments = await commentsData.json();
  const ul = document.createElement('ul');
  for (const comment of comments) {
    const li = document.createElement('li');
    li.innerText = comment.content;
    if (comment.imageURL) {
      li.appendChild(document.createElement('br'));
      const img = document.createElement('img');
      img.setAttribute('src', comment.imageURL);
      li.appendChild(img);
    }
    ul.appendChild(li);
  }
  document.getElementById('comments-container').innerHTML = '';
  document.getElementById('comments-container').appendChild(ul);
}

async function deleteComments() {
  await fetch('/delete-data', {
    method: 'POST',
  });
  getComments();
}
