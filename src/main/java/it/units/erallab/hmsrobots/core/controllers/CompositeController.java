package it.units.erallab.hmsrobots.core.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CompositeController extends AbstractController {
  @JsonProperty
  protected final AbstractController innerController;

  protected CompositeController(@JsonProperty("innerController") AbstractController innerController) {
    this.innerController = innerController;
  }

  public AbstractController getInnermostController() {
    return innerController instanceof CompositeController cc ? cc.getInnermostController() : innerController;
  }

}
