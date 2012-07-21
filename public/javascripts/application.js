

Fontacts = Ember.Application.create({
  rootElement: 'body',
  ready: function() {
    // Call the superclass's `ready` method.
    this._super();
  },
  entry_mode: 1,
  current_contact: null,
  show_entry_form: function() {
    $('body').animate({scrollTop: 0}, 200, function() {
      $('.new-contact').stop().animate({opacity: 1, marginLeft: '20px', scrollTop: 0}, 700);
    });
  }
});


// Ember object model for contacts
Fontacts.Contact = Em.Object.extend({
  id: null,
  fullname: null,
  email: null,
  phone: null,
  imageUrl: null,
  computeUrl: function() {
    return jsRoutes.controllers.Application.contact_photo(this.get('id'), this.get('imageUrl')).url;
  }.property('imageUrl')
});


// Contact Controller
Fontacts.contactController = Em.ArrayController.create({
  // content array for Ember's data
  content: [],

  // array to cache initial contact items
  main_content: [],

  // fetches all contacts from the server
  fetch: function() {
    var fetchRoute = jsRoutes.controllers.Application.all(),
    self = this;

    $.ajax({
      url: fetchRoute.url,
      dataType: 'json',
      method: fetchRoute.method,
      success: function(result) {
        var contacts = result.contacts.map(function(contact) {
          return Fontacts.Contact.create(contact);
        });
        //$('.contacts .temp').hide().remove();
        self.set('content', contacts);
        self.set('main_content', contacts);
      }
    });
  },

  // adds a contact to the controller
  add: function(json) {
    // create an Ember model from the passed json object
    // and add it to the contact controller
    var contact = Fontacts.Contact.create(json)
    this.insertAt(0, contact);
  },

  delete: function(contact) {
    var self = this,
        _id = contact.get('id'),
        deleteRoute = jsRoutes.controllers.Application.delete(_id);
    $.ajax({
      url: deleteRoute.url,
      type: deleteRoute.method,
      success: function(result) {
        self.removeObject(contact);
      }
    });
  },

  filterContacts: function(criteria) {
    // retrieve all arguments
    var args = arguments;

    // if there are arguments
    if (args.length > 1) {
      // filter the contacts by the criteria
      var contacts = this.get('main_content').filter(function(item) {
        // go through the list of search parameters
        for (var i = 1; i < args.length; i++) {
          // get a contact property based on the search parameter
          var value = item.get(args[i]);

          // if a contact was gotten
          if (value) {
            value = value.toString();
            // add the contact to the filter if it matches the search criteria
            var found = value.toLowerCase().indexOf(criteria.toLowerCase()) !== -1

            if (found) return found;
          }
        }

        return false;
      });

      // set the content of the controller to the filtered contacts
      this.set('content', contacts);
    }
  },

  clearFilter: function() {
    this.set('content', this.get('main_content'));
  },

  // Checks if the content of the controller is empty
  isEmpty: function() {
    return this.get('length') === 0;
  }.property('@each')
});
Fontacts.contactController.fetch();


// Top bar view
Fontacts.TopBar = Ember.View.extend({
  add_new: function(e) {
    e.preventDefault();
    // set the entry mode to create/new.
    Fontacts.set('entry_mode', 1);
    Fontacts.show_entry_form();
  },

  do_filter: function() {
    // get the search criteria
    var criteria = $('.search-box input').val();

    // reset the view if the search criteria is empty
    if (criteria === "") {
      Fontacts.contactController.clearFilter();
    } else {
      // filter contacts based on the search criteria
      Fontacts.contactController.filterContacts(criteria, "fullname", "email", "phone")
    }
  }
});


// Contact entry view
Fontacts.NewContactView = Ember.View.extend({
  validate: function() {
    var $contactForm = $('#new-contact-form'),
        $fullName = $contactForm.find('input[name=fullname]').val(),
        $mobile = $contactForm.find('input[name=mobile]').val();

    if ($fullName === '') {
      return "Enter full name.";
    } else if ($mobile === '') {
      return "Mobile number is required.";
    } else {
      return null;
    }
  },

  // saves a new contact
  save_contact: function(e) {
    e.preventDefault();

    var self = this,
        $form = $('#new-contact-form'),
        error = self.validate();

    // if there is an error then display it
    if (error) {
      alert(error);
    } else {

      if (Fontacts.get('entry_mode') === 1) {
        // retrieve the route for adding new contacts
        var addRoute = jsRoutes.controllers.Application.add();

        // submit the contact form using ajax
        $form.ajaxSubmit({
          url: addRoute.url,
          dataType: 'json',
          method: addRoute.method,
          // shows the contact if it was save successfully
          success: function(contact) {
            if (contact && contact.fullname) {
              self.close_entry_form(e);
              Fontacts.contactController.add(contact);
            }
          }
        });
      } else {
        // retrieve the route for editing contacts
        var contact = Fontacts.get('current_contact');
        var _id = contact.get('id');
        var editRoute = jsRoutes.controllers.Application.edit(_id);

        $form.ajaxSubmit({
          url: editRoute.url,
          dataType: 'json',
          method: editRoute.method,
          success: function(result) {
            if (result && result.fullname) {

              contact.set('fullname', result.fullname);
              contact.set('phone', result.phone);
              contact.set('email', result.email);
              contact.set('imageUrl', result.imageUrl)
            }

            self.close_entry_form(e);
          }
        });
      }
    }
  },

  // hides and reset the form
  close_entry_form: function(e) {
    e.preventDefault();
    $('#new-contact-form').resetForm();
    Fontacts.set('current_contact', null);
    $('.new-contact').stop().animate({opacity: 0, marginLeft: '-260px'}, 700);
  }
});


// Contact View
Fontacts.ContactView = Ember.View.extend({
  classNames: ['contact'],
  edit: function(event) {
    // set the current mode to edit.
    Fontacts.set('entry_mode', 2);

    var $form = $('#new-contact-form'),
        $target = $(event.target),
    // get the id of the current contact
        _id = $target.closest('.contact').attr('id'),
    // find the contact with the retrieved id
        contact = Fontacts.contactController.findProperty('id', _id);
    // set the contact to the current contact being edited
    Fontacts.set('current_contact', contact);

    // show the data entry box.
    Fontacts.show_entry_form();
  },
  delete: function(event) {
    var $target = $(event.target),
        _id = $target.closest('.contact').attr('id'),
        contact = Fontacts.contactController.findProperty('id', _id);

    if(confirm("Are you sure you want to delete this contact?")) {
      Fontacts.contactController.delete(contact)
    }
  }
});