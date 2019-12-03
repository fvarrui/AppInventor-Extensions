// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.explorer.youngandroid;

import com.google.appinventor.client.ErrorReporter;
import com.google.appinventor.client.GalleryClient;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.boxes.ProjectListBox;
import com.google.appinventor.client.boxes.TrashProjectListBox;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.client.widgets.Toolbar;
import com.google.appinventor.client.wizards.youngandroid.NewYoungAndroidProjectWizard;
import com.google.appinventor.shared.rpc.project.GalleryApp;
import com.google.appinventor.shared.rpc.project.GallerySettings;
import com.google.appinventor.shared.rpc.project.UserProject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;

import java.util.List;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * The project toolbar houses command buttons in the Young Android Project tab.
 *
 */
public class ProjectToolbar extends Toolbar {
  private static final String WIDGET_NAME_NEW = "New";
  private static final String WIDGET_NAME_DELETE = "Delete";
  private static final String WIDGET_NAME_PUBLISH_OR_UPDATE = "PublishOrUpdate";
  private static final String WIDGET_NAME_TRASH = "Trash";
  private static final String WIDGET_NAME_PROJECT= "Projects";
  private static final String WIDGET_NAME_RESTORE= "Restore";
  private static final String WIDGET_NAME_DELETE_FROM_TRASH= "Delete From Trash";

  private boolean isReadOnly;

  /**
   * Initializes and assembles all commands into buttons in the toolbar.
   */
  public ProjectToolbar() {
    super();
    isReadOnly = Ode.getInstance().isReadOnly();

    addButton(new ToolbarItem(WIDGET_NAME_NEW, MESSAGES.newProjectMenuItem(),
        new NewAction(this)));

    addButton(new ToolbarItem(WIDGET_NAME_DELETE, MESSAGES.deleteProjectButton(),
        new DeleteAction()));
    addButton(new ToolbarItem(WIDGET_NAME_PUBLISH_OR_UPDATE, MESSAGES.publishToGalleryButton(),
        new PublishOrUpdateAction()));
    addButton(new ToolbarItem(WIDGET_NAME_TRASH,MESSAGES.trashButton(),
        new TrashAction()));
    addButton(new ToolbarItem(WIDGET_NAME_PROJECT,MESSAGES.myProjectsButton(),
        new BackToProjectViewAction()));
    addButton(new ToolbarItem(WIDGET_NAME_RESTORE,MESSAGES.restoreProjectButton(),
        new RestoreProjectAction()));
    addButton(new ToolbarItem(WIDGET_NAME_DELETE_FROM_TRASH,MESSAGES.deleteFromTrashButton(),
        new DeleteForeverProjectAction()));

    setTrashTabButtonsVisible(false);
    updateButtons();
  }

  public void setPublishOrUpdateButtonVisible(boolean visible){
    setButtonVisible(WIDGET_NAME_PUBLISH_OR_UPDATE, visible);
  }

  public void setTrashTabButtonsVisible(boolean visible) {
    setButtonVisible(WIDGET_NAME_PROJECT, visible);
    setButtonVisible(WIDGET_NAME_RESTORE, visible);
    setButtonVisible(WIDGET_NAME_DELETE_FROM_TRASH, visible);
    updateTrashButtons();
  }

  public void setProjectTabButtonsVisible(boolean visible) {
    setButtonVisible(WIDGET_NAME_NEW, visible);
    setButtonVisible(WIDGET_NAME_TRASH,visible);
    setButtonVisible(WIDGET_NAME_DELETE,visible);
  }

  private static class NewAction implements Command {
    ProjectToolbar parent;

    public NewAction(ProjectToolbar parent) {
      this.parent = parent;
    }

    @Override
    public void execute() {
      if (Ode.getInstance().screensLocked()) {
        return;                 // Refuse to switch if locked (save file happening)
      }
      // Disabled the Start New Project button. We do this because on slow machines people
      // click it multiple times while the wizard (below) is starting. This then causes
      // a second wizard to start and a very confused user experience.
      // We will enable the button again when we re-visit the Project List page
      parent.setButtonEnabled(WIDGET_NAME_NEW, false);
      new NewYoungAndroidProjectWizard(parent).center();
      // The wizard will switch to the design view when the new
      // project is created.
    }
  }

  private static class DeleteAction implements Command {
    @Override
    public void execute() {
      Ode.getInstance().getEditorManager().saveDirtyEditors(new Command() {
        @Override
        public void execute() {
          List<Project> selectedProjects =
              ProjectListBox.getProjectListBox().getProjectList().getSelectedProjects();
          if (selectedProjects.size() > 0) {
            // Show one confirmation window for selected projects.
            if (deleteConfirmation(selectedProjects)) {
              for (Project project : selectedProjects) {
                moveToTrash(project);
              }
            }
          } else {
            // The user can select a project to resolve the
            // error.
            ErrorReporter.reportInfo(MESSAGES.noProjectSelectedForDelete());
          }
        }
      });
    }

    private boolean deleteConfirmation(List<Project> projects) {
      String message;
      GallerySettings gallerySettings = GalleryClient.getInstance().getGallerySettings();
      if (projects.size() == 1) {
        if (projects.get(0).isPublished()) {
          message = MESSAGES.confirmDeleteSinglePublishedProjectWarning(projects.get(0).getProjectName());
        } else {
          message = MESSAGES.confirmMoveToTrashSingleProject(projects.get(0).getProjectName());
        }
      } else {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (Project project : projects) {
          sb.append(separator).append(project.getProjectName());
          separator = ", ";
        }
        String projectNames = sb.toString();
        if(!gallerySettings.galleryEnabled()){
          message = MESSAGES.confirmMoveToTrash(projectNames);
        } else {
          message = MESSAGES.confirmDeleteManyProjectsWithGalleryOn(projectNames);
        }
      }
      return Window.confirm(message);
    }

    private void moveToTrash(Project project) {
      Tracking.trackEvent(Tracking.PROJECT_EVENT,
          Tracking.PROJECT_ACTION_MOVE_TO_TRASH_PROJECT_YA, project.getProjectName());

      final long projectId = project.getProjectId();

      // Make sure that we delete projects even if they are not open.
      doMoveProjectToTrash(projectId);
    }

    private void doMoveProjectToTrash(final long projectId) {
      Ode.getInstance().getProjectService().moveToTrash(projectId,
          new OdeAsyncCallback<UserProject>(
              // failure message
              MESSAGES.moveToTrashProjectError()) {
            @Override
            public void onSuccess(UserProject project) {
              if(project.getProjectId()== projectId){
                Ode.getInstance().getProjectManager().removeProject(projectId);
                Ode.getInstance().getProjectManager().addDeletedProject(project);
                if (Ode.getInstance().getProjectManager().getDeletedProjects().size() == 0) {
                  Ode.getInstance().createEmptyTrashDialog(true);
                }
              }
            }
          });
    }
  }

  //implementing trash method this method will show the Trash Tab
  private static class TrashAction implements Command {
    @Override
    public void execute() {
      Ode.getInstance().getEditorManager().saveDirtyEditors(new Command() {
        @Override
        public void execute() {
          Ode.getInstance().switchToTrash();
        }
      });
    }
  }

  //Moving Back From Trash Tab To Projects Tab
  private static class BackToProjectViewAction implements Command {
    @Override
    public void execute() {
      Ode.getInstance().getEditorManager().saveDirtyEditors(new Command() {
        @Override
        public void execute() {
          Ode.getInstance().switchToProjectsView();
        }
      });
    }
  }

  //Restoring the project back to My Projects from Trash Can
  private static class RestoreProjectAction implements Command {
    @Override
    public void execute() {
      List<Project> selectedProjects =
          TrashProjectListBox.getTrashProjectListBox().getTrashProjectList().getSelectedProjects();
      if (selectedProjects.size() > 0) {
        for (Project project : selectedProjects){
          restoreProject(project);
        }
      } else {
        // The user can select a project to resolve the
        // error.
        ErrorReporter.reportInfo(MESSAGES.noProjectSelectedForRestore());
      }
    }

    private void restoreProject(Project project) {
      Tracking.trackEvent(Tracking.PROJECT_EVENT,
          Tracking.PROJECT_ACTION_RESTORE_PROJECT_YA, project.getProjectName());

      final long projectId = project.getProjectId();

      doRestoreProject(projectId);
    }

    private void doRestoreProject(final long projectId) {
      Ode.getInstance().getProjectService().restoreProject(projectId,
          new OdeAsyncCallback<UserProject>(
              // failure message
              MESSAGES.restoreProjectError()) {
            @Override
            public void onSuccess(UserProject project) {
              if (project.getProjectId() == projectId) {
                Ode.getInstance().getProjectManager().restoreDeletedProject(projectId);
                if (Ode.getInstance().getProjectManager().getDeletedProjects().size() == 0) {
                  Ode.getInstance().createEmptyTrashDialog(true);
                }
              }
            }
          });
    }
  }

  //Deleting the projects forever from trash list
  private static class DeleteForeverProjectAction implements Command {
    @Override
    public void execute() {
      Ode.getInstance().getEditorManager().saveDirtyEditors(new Command() {
        @Override
        public void execute() {
          List<Project> deletedProjects =
              TrashProjectListBox.getTrashProjectListBox().getTrashProjectList().getSelectedProjects();
          if (deletedProjects.size() > 0) {
            // Show one confirmation window for selected projects.
            if (deleteConfirmation(deletedProjects)) {
              for (Project project : deletedProjects) {
                deleteProject(project);
              }
            }
          } else {
            // The user can select a project to resolve the
            // error.
            ErrorReporter.reportInfo(MESSAGES.noProjectSelectedForDelete());
          }
        }
      });
    }

    private boolean deleteConfirmation(List<Project> projects) {
      String message;
      GallerySettings gallerySettings = GalleryClient.getInstance().getGallerySettings();
      if (projects.size() == 1) {
        if (projects.get(0).isPublished()) {
          message = MESSAGES.confirmDeleteSinglePublishedProject(projects.get(0).getProjectName());
        } else {
          message = MESSAGES.confirmDeleteSingleProject(projects.get(0).getProjectName());
        }
      } else {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (Project project : projects) {
          sb.append(separator).append(project.getProjectName());
          separator = ", ";
        }
        String projectNames = sb.toString();
        if(!gallerySettings.galleryEnabled()){
          message = MESSAGES.confirmDeleteManyProjects(projectNames);
        } else {
          message = MESSAGES.confirmDeleteForeverManyProjectsWithGalleryOn(projectNames);
        }
      }
      return Window.confirm(message);
    }

    private void deleteProject(Project project) {
      Tracking.trackEvent(Tracking.PROJECT_EVENT,
          Tracking.PROJECT_ACTION_DELETE_PROJECT_YA, project.getProjectName());

      final long projectId = project.getProjectId();
      if (project.isPublished()) {
        doDeleteGalleryApp(project.getGalleryId());
      }
      // Make sure that we delete projects even if they are not open.
      doDeleteProject(projectId);
    }

    private void doDeleteProject(final long projectId) {
      Ode.getInstance().getProjectService().deleteProject(projectId,
          new OdeAsyncCallback<Void>(
              // failure message
              MESSAGES.deleteProjectError()) {
            @Override
            public void onSuccess(Void result) {
              Ode.getInstance().getProjectManager().removeDeletedProject(projectId);
              // Show a welcome dialog in case there are no
              // projects saved.
              if (Ode.getInstance().getProjectManager().getDeletedProjects().size() == 0) {
                Ode.getInstance().createEmptyTrashDialog(true);
              }
            }
          });
    }

    private void doDeleteGalleryApp(final long galleryId) {
      Ode.getInstance().getGalleryService().deleteApp(galleryId,
          new OdeAsyncCallback<Void>(
              // failure message
              MESSAGES.galleryDeleteError()) {
            @Override
            public void onSuccess(Void result) {
              // need to update gallery list
              GalleryClient gallery = GalleryClient.getInstance();
              gallery.appWasChanged();
            }
          });
    }
  }

  private static class PublishOrUpdateAction implements Command {
    @Override
    public void execute() {
      List<Project> selectedProjects =
          ProjectListBox.getProjectListBox().getProjectList().getSelectedProjects();
      if (selectedProjects.size() == 1) {
        Project currentSelectedProject = ProjectListBox.getProjectListBox().getProjectList()
            .getSelectedProjects().get(0);
        if(!currentSelectedProject.isPublished()){
          // app is not yet published
          publishToGallery(currentSelectedProject);
        }else{
          updateGalleryApp(currentSelectedProject);
        }
      } else {
        // The publish/update button will be disabled if selectedProjects.size != 1
        // This should not happen, but just in case

        ErrorReporter.reportInfo(MESSAGES.wrongNumberProjectSelectedForPublishOrUpdate());
      }
    }

    private void publishToGallery(Project p) {
      // first create an app object with default data
      final GalleryApp app = new GalleryApp(p.getProjectName(), p.getProjectId(),
          p.getProjectName(), p.getGalleryId(), p.getAttributionId());
      Ode.getInstance().switchToGalleryAppView(app, GalleryPage.NEWAPP);
    }

    private void updateGalleryApp(Project p) {
      // setup what happens when we load the app in
      final OdeAsyncCallback<GalleryApp> callback = new OdeAsyncCallback<GalleryApp>(
          MESSAGES.galleryError()) {
        @Override
        public void onSuccess(GalleryApp app) {
          // the server has returned us something
          int editStatus=GalleryPage.UPDATEAPP;
          Ode.getInstance().switchToGalleryAppView(app, editStatus);
        }
      };
      // ok, this is below the call back, but of course it is done first
      Ode.getInstance().getGalleryService().getApp(p.getGalleryId(),callback);
    }
  }

  /**
   * Enables and/or disables buttons based on how many projects exist
   * (in the case of "Download All Projects") or are selected (in the case
   * of "Delete" and "Download Source").
   */
  public void updateButtons() {
    ProjectList projectList = ProjectListBox.getProjectListBox().getProjectList();
    int numProjects = projectList.getNumProjects();
    int numSelectedProjects = projectList.getNumSelectedProjects();
    if (isReadOnly) {           // If we are read-only, we disable all buttons
      setButtonText(WIDGET_NAME_PUBLISH_OR_UPDATE, MESSAGES.publishToGalleryButton());
      setButtonEnabled(WIDGET_NAME_NEW, false);
      setButtonEnabled(WIDGET_NAME_DELETE, false);
      setButtonEnabled(WIDGET_NAME_PUBLISH_OR_UPDATE, false);
      Ode.getInstance().getTopToolbar().updateMenuState(numSelectedProjects, numProjects);
      return;
    }
    setButtonEnabled(WIDGET_NAME_DELETE, numSelectedProjects > 0);
    setButtonEnabled(WIDGET_NAME_PUBLISH_OR_UPDATE, numSelectedProjects == 1);
    if (numSelectedProjects == 1 && ProjectListBox.getProjectListBox().getProjectList()
        .getSelectedProjects().get(0).isPublished()){
      setButtonText(WIDGET_NAME_PUBLISH_OR_UPDATE, MESSAGES.updateGalleryAppButton());
    } else {
      setButtonText(WIDGET_NAME_PUBLISH_OR_UPDATE, MESSAGES.publishToGalleryButton());
    }
    Ode.getInstance().getTopToolbar().updateMenuState(numSelectedProjects, numProjects);
  }

  public void updateTrashButtons() {
    TrashProjectList trashProjectList = TrashProjectListBox.getTrashProjectListBox().getTrashProjectList();
    int numSelectedProjects = trashProjectList.getNumSelectedProjects();
    if (isReadOnly) {           // If we are read-only, we disable all buttons
      setButtonEnabled(WIDGET_NAME_DELETE_FROM_TRASH, false);
      setButtonEnabled(WIDGET_NAME_RESTORE, false);
      return;
    }
    setButtonEnabled(WIDGET_NAME_DELETE_FROM_TRASH, numSelectedProjects > 0);
    setButtonEnabled(WIDGET_NAME_RESTORE, numSelectedProjects > 0);
  }

  // If we started a project, then the start button was disabled (to avoid
  // a second press while the new project wizard was starting (aka we "debounce"
  // the button). When the person switches to the projects list view again (here)
  // we re-enable it.
  public void enableStartButton() {
    if (!isReadOnly) {
      setButtonEnabled(WIDGET_NAME_NEW, true);
    }
  }

}
